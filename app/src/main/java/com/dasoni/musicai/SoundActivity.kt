package com.dasoni.musicai

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.database.FirebaseDatabase
import kotlin.math.absoluteValue

class SoundActivity : AppCompatActivity() {
    private var audioThread: Thread? = null
    private var isListening = false
    private var audioRecord: AudioRecord? = null
    private lateinit var statusText: TextView
    private lateinit var bpmView: TextView
    private var lastDetectedTime = 0L
    private val cooldownMs = 80
    private lateinit var playView: View
    private lateinit var markHit: () -> Unit
    private val hitCircles = mutableListOf<Float>()
    private val targetCircles = mutableListOf<Float>()
    private var barX = 0f
    private var targetReady = false
    private var startTime = 0L
    private var timestamps = listOf<Long>()
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        setContentView(R.layout.detect_sound)

        statusText = findViewById(R.id.soundStatus)
        bpmView = findViewById(R.id.bpmView)
        val container = findViewById<FrameLayout>(R.id.container)
        playView = object : View(this) {
            private val barPaint = Paint().apply {
                color = Color.RED
                strokeWidth = 6f
            }
            private val userPaint = Paint().apply {
                color = Color.BLACK
                isAntiAlias = true
            }
            private val targetPaint = Paint().apply {
                color = Color.GRAY
                isAntiAlias = true
            }

            private val frameRate = 60

            private val animationLoop = object : Runnable {
                override fun run() {
                    if (!targetReady || timestamps.size < 2) {
                        postDelayed(this, 1000L / frameRate)
                        return
                    }
                    val elapsed = System.currentTimeMillis() - startTime
                    barX = calculateBarX(elapsed)
                    invalidate()
                    postDelayed(this, 1000L / frameRate)
                }
            }

            init {
                markHit = { markHit() }
                post(animationLoop)
            }

            fun markHit() {
                hitCircles.add(barX)
                invalidate()
            }

            override fun onDraw(canvas: Canvas) {
                super.onDraw(canvas)
                canvas.drawLine(barX, 0f, barX, height.toFloat(), barPaint)
                targetCircles.forEach {
                    canvas.drawCircle(it, height / 3f, 16f, targetPaint)
                }
                hitCircles.forEach {
                    canvas.drawCircle(it, 2 * height / 3f, 20f, userPaint)
                }
            }

            override fun onTouchEvent(event: MotionEvent): Boolean {
                if (event.action == MotionEvent.ACTION_DOWN) {
                    hitCircles.add(barX)
                    invalidate()
                    return true
                }
                return super.onTouchEvent(event)
            }
        }
        container.addView(playView)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        } else {
            startListening()
        }

        loadRandomSongFromFirebase()
    }

    private fun loadRandomSongFromFirebase() {
        val db = FirebaseDatabase.getInstance()
        val songsRef = db.getReference("MUSICAI/songs")

        songsRef.get().addOnSuccessListener { snapshot ->
            val songIds = snapshot.children.mapNotNull { it.key }
            if (songIds.isNotEmpty()) {
                val randomSongId = songIds.random()
                val baseName = randomSongId.removePrefix("song_")
                val songRef = songsRef.child(randomSongId)
                songRef.get().addOnSuccessListener { songSnap ->
                    val bpm = songSnap.child("bpm").getValue(Int::class.java) ?: 0
                    bpmView.text = "BPM: $bpm"
                    val timestampsSnap = songSnap.child("timestamps")
                    timestamps = timestampsSnap.children.mapNotNull { it.getValue(Long::class.java) }
                    setupTargetCircles(timestamps)
                    playAudioFromAssets(baseName)
                    Toast.makeText(this, "곡: $randomSongId", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupTargetCircles(timestamps: List<Long>) {
        val viewWidth = playView.width.takeIf { it > 0 } ?: return
        val margin = 24f
        val usableWidth = viewWidth - (2 * margin)

        targetCircles.clear()
        val first = timestamps.first()
        val last = timestamps.last()
        val durationMs = last - first

        timestamps.forEach { time ->
            val progress = (time - first).toFloat() / durationMs
            targetCircles.add(margin + progress * usableWidth)
        }

        playView.invalidate()
    }

    private fun calculateBarX(elapsed: Long): Float {
        if (timestamps.size < 2) return 0f
        val margin = 24f
        val usableWidth = playView.width - 2 * margin
        val first = timestamps.first()
        val last = timestamps.last()
        val totalDuration = last - first

        val offsetMs = -250L
        val adjustedElapsed = (elapsed + offsetMs - first).coerceAtLeast(0)
        val progress = adjustedElapsed.toFloat() / totalDuration.coerceAtLeast(1)
        return margin + progress * usableWidth
    }

    private fun playAudioFromAssets(baseName: String) {
        val extensions = listOf(".wav", ".mp3")
        for (ext in extensions) {
            val fileName = "$baseName$ext"
            try {
                val afd = assets.openFd(fileName)
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    prepare()
                    setOnPreparedListener {
                        startTime = System.currentTimeMillis()
                        targetReady = true
                        start()
                    }
                }
                return
            } catch (e: Exception) {
                Log.d("Audio", "파일 없음 또는 에러: $fileName")
            }
        }
        Toast.makeText(this, "오디오 파일을 찾을 수 없습니다", Toast.LENGTH_SHORT).show()
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
                        if ((peak > 10000 || avg > 5000) && (now - lastDetectedTime > cooldownMs)) {
                            lastDetectedTime = now
                            runOnUiThread {
                                Log.d("Beat", "Detected at \${now}")
                                markHit()
                            }
                        }
                    }
                    Thread.sleep(5)
                }
            } catch (e: Exception) {
                Log.e("SoundDebug", "Thread crashed: \${e.message}", e)
                runOnUiThread {
                    statusText.text = "Error: \${e.message}"
                }
            }
        }
        audioThread?.start()
    }

    private fun stopListening() {
        isListening = false
        audioThread?.join(100)
        audioRecord?.apply {
            if (recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                stop()
                Log.d("SoundDebug", "AudioRecord stopped")
            }
            release()
            Log.d("SoundDebug", "AudioRecord released")
        }
        audioRecord = null
        mediaPlayer?.release()
        mediaPlayer = null
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startListening()
        } else {
            statusText.text = "Permission denied"
        }
    }
}