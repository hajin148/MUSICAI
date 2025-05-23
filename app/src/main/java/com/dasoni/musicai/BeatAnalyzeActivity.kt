package com.dasoni.musicai

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

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

    private fun sendFileToServer(uri: Uri) {
        val contentResolver = contentResolver
        val inputStream = contentResolver.openInputStream(uri)
        val file = File(cacheDir, "selected_audio.wav") // 혹은 .mp3
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

                    runOnUiThread {
                        Toast.makeText(this@BeatAnalyzeActivity, "받은 비트: ${beatList.size}개", Toast.LENGTH_SHORT).show()
                        Log.d("BeatList", beatList.toString())
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
