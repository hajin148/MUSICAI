package com.dasoni.musicai

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlin.math.absoluteValue
import android.util.Log

class SoundActivity : AppCompatActivity() {

    private var audioThread: Thread? = null
    private var isListening = false
    private var audioRecord: AudioRecord? = null
    private lateinit var statusText: TextView
    private var lastDetectedTime = 0L
    private val cooldownMs = 80

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.detect_sound)

        statusText = findViewById(R.id.soundStatus)

        // get microphone permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        } else {
            startListening()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startListening()
        } else {
            statusText.text = "Permission denied"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopListening()
    }

    private fun startListening() {
        if (isListening) return

        isListening = true

        val bufferSize = AudioRecord.getMinBufferSize(
            44100,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val adjustedBufferSize = maxOf(bufferSize / 2, 1024)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                44100,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                adjustedBufferSize
            )
            audioRecord?.startRecording()
        } catch (e: SecurityException) {
            e.printStackTrace()
            statusText.text = "Mic permission denied"
            return
        }

        val buffer = ShortArray(adjustedBufferSize)
        audioThread = Thread {
            try {
                while (isListening) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        val peak = buffer.maxOf { it.toInt().absoluteValue }
                        val avg = buffer.map { it.toInt().absoluteValue }.average()
                        val now = System.currentTimeMillis()
                        if ((peak > 6000 || avg > 2500) && (now - lastDetectedTime > cooldownMs)) {
                            lastDetectedTime = now
                            runOnUiThread {
                                statusText.text = "🔊"
                                Log.d("Beat", "Detected at ${now}")
                            }
                        } else {
                            runOnUiThread {
                                statusText.text = "Listening..."
                            }
                        }
                    }
                    Thread.sleep(30)
                }
            } catch (e: Exception) {
                Log.e("SoundDebug", "Thread crashed: ${e.message}", e)
                runOnUiThread {
                    statusText.text = "Error: ${e.message}"
                }
            }
        }
        audioThread?.start()
    }

    private fun stopListening() {
        isListening = false
        audioThread?.join(100) // wait for the thread to stop

        audioRecord?.apply {
            if (recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                stop()
                Log.d("SoundDebug", "AudioRecord stopped")
            }
            release()
            Log.d("SoundDebug", "AudioRecord released")
        }
        audioRecord = null
    }


    override fun onPause() {
        super.onPause()
        stopListening()
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startListening()
        }
    }
}
