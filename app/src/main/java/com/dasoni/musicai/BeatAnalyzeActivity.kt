package com.dasoni.musicai

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import com.google.firebase.database.FirebaseDatabase
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.roundToInt

class BeatAnalyzeActivity : AppCompatActivity() {

    private val PICK_AUDIO_REQUEST = 1001
    private lateinit var beatList: ArrayList<Long>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_beat_analyze)

        val pickButton = findViewById<Button>(R.id.btnPickAudio)
        pickButton.setOnClickListener {
            pickAudioFile()
        }
    }

    private fun pickAudioFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "audio/*"
        startActivityForResult(intent, PICK_AUDIO_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_AUDIO_REQUEST && resultCode == RESULT_OK && data != null) {
            val uri = data.data
            uri?.let {
                sendFileToServer(it)
            }
        }
    }

    private fun calculateBPM(beats: List<Long>): Int {
        if (beats.size < 2) return 0
        val intervals = beats.zipWithNext { a, b -> b - a }
        val avg = intervals.average()
        return (60000 / avg).roundToInt()
    }

    private fun getFileNameFromUri(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    result = it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }

    private fun sendFileToServer(uri: Uri) {
        val contentResolver = contentResolver

        val fileName = getFileNameFromUri(uri) ?: "untitled"
        var songName = fileName.substringBeforeLast(".")
            .replace(" ", "_")
            .replace(Regex("[^a-zA-Z0-9_]"), "")
            .lowercase()

        if (songName.matches(Regex("^\\d+$"))) {
            songName = "song_$songName"
        }

        val inputStream = contentResolver.openInputStream(uri)
        val file = File(cacheDir, "temp_audio.wav")
        val outputStream = FileOutputStream(file)
        inputStream?.copyTo(outputStream)
        inputStream?.close()
        outputStream.close()

        val client = OkHttpClient()
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                file.name,
                file.asRequestBody("audio/wav".toMediaTypeOrNull())
            )
            .build()

        val request = Request.Builder()
            .url("https://beat-analyzer-production.up.railway.app/analyze")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@BeatAnalyzeActivity, "업로드 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                beatList = ArrayList()

                try {
                    val json = JSONObject(responseBody)
                    val jsonArray = json.getJSONArray("beat_times_ms")
                    for (i in 0 until jsonArray.length()) {
                        beatList.add(jsonArray.getLong(i))
                    }

                    val bpm = calculateBPM(beatList)

                    val database = FirebaseDatabase.getInstance()
                    val songRef = database.getReference("MUSICAI/songs/$songName")

                    val songData = mapOf(
                        "bpm" to bpm,
                        "rhythm" to "N/A",
                        "timestamps" to beatList
                    )

                    songRef.setValue(songData)
                        .addOnSuccessListener {
                            Toast.makeText(this@BeatAnalyzeActivity, "Firebase 저장 완료", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this@BeatAnalyzeActivity, "DB 저장 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                        }

                    runOnUiThread {
                        Toast.makeText(this@BeatAnalyzeActivity, "받은 비트: ${beatList.size}개", Toast.LENGTH_SHORT).show()
                        Log.d("BeatList", beatList.toString())

                        val container = findViewById<FrameLayout>(R.id.visualizer_container)
                        container.removeAllViews()
                        container.addView(BeatVisualizerView(this@BeatAnalyzeActivity, beatList))
                    }

                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this@BeatAnalyzeActivity, "응답 파싱 오류", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

}
