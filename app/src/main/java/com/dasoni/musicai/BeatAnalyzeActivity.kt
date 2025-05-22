package com.dasoni.musicai

import android.media.*
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.nio.ByteBuffer
import kotlin.math.log10

class BeatAnalyzeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val audioFile = copyAssetToInternalStorage("1.wav")
        val pcmBytes = decodeWavWithMediaExtractor(audioFile.absolutePath)
        val beats = estimateBeats(pcmBytes, 44100)
        Log.d("BeatEstimation", "Detected beat times (s): $beats")
    }

    private fun copyAssetToInternalStorage(fileName: String): File {
        val outFile = File(filesDir, fileName)
        if (!outFile.exists()) {
            assets.open(fileName).use { input -> outFile.outputStream().use { output -> input.copyTo(output) } }
        }
        return outFile
    }

    private fun decodeWavWithMediaExtractor(filePath: String): ByteArray {
        val extractor = MediaExtractor()
        extractor.setDataSource(filePath)

        var audioTrackIndex = -1
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            if (format.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                audioTrackIndex = i
                break
            }
        }
        if (audioTrackIndex == -1) throw IllegalStateException("No audio track found")

        extractor.selectTrack(audioTrackIndex)
        val format = extractor.getTrackFormat(audioTrackIndex)
        val codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
        codec.configure(format, null, null, 0)
        codec.start()

        val inputBuffers = codec.inputBuffers
        val outputBuffers = codec.outputBuffers
        val bufferInfo = MediaCodec.BufferInfo()
        val pcmData = mutableListOf<Byte>()

        var isEOS = false
        while (true) {
            if (!isEOS) {
                val inIndex = codec.dequeueInputBuffer(10000)
                if (inIndex >= 0) {
                    val buffer = inputBuffers[inIndex]
                    val sampleSize = extractor.readSampleData(buffer, 0)
                    if (sampleSize < 0) {
                        codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        isEOS = true
                    } else {
                        codec.queueInputBuffer(inIndex, 0, sampleSize, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }

            val outIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
            if (outIndex >= 0) {
                val outBuffer: ByteBuffer = outputBuffers[outIndex]
                val bytes = ByteArray(bufferInfo.size)
                outBuffer.get(bytes)
                outBuffer.clear()
                pcmData.addAll(bytes.toList())
                codec.releaseOutputBuffer(outIndex, false)
            } else if (outIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                break
            }

            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
        }

        codec.stop()
        codec.release()
        extractor.release()

        return pcmData.toByteArray()
    }

    private fun estimateBeats(pcmBytes: ByteArray, sampleRate: Int): List<Float> {
        val beatTimes = mutableListOf<Float>()
        val samples = ByteBuffer.wrap(pcmBytes).asShortBuffer()

        val channelCount = 2
        val rawSamples = ShortArray(samples.limit())
        samples.get(rawSamples)

        val audioData = ShortArray(rawSamples.size / channelCount)
        for (i in audioData.indices) {
            val left = rawSamples[i * 2]
            val right = rawSamples[i * 2 + 1]
            audioData[i] = ((left + right) / 2).toShort()
        }

        val frameSize = 1024
        val hopSize = 512
        val numFrames = (audioData.size - frameSize) / hopSize

        val energies = DoubleArray(numFrames) { i ->
            var energy = 0.0
            for (j in 0 until frameSize) {
                val sample = audioData[i * hopSize + j].toDouble()
                energy += sample * sample
            }
            energy
        }

        val logEnergies = energies.map { 10 * log10(it + 1e-6) }
        val minEnergy = logEnergies.minOrNull() ?: 0.0
        val maxEnergy = logEnergies.maxOrNull() ?: 1.0
        val normEnergies = logEnergies.map { (it - minEnergy) / (maxEnergy - minEnergy) }

        val smoothed = normEnergies.toMutableList()
        for (i in 2 until normEnergies.size - 2) {
            val window = normEnergies.subList(i - 2, i + 3).sorted()
            smoothed[i] = window[2]
        }

        val minInterval = (0.7f * sampleRate / hopSize).toInt()
        var lastPeak = -minInterval
        val threshold = 0.35
        val timeZeroCorrection = -0.3f  // 필요 시 보정값 유지

        for (i in 1 until smoothed.size - 1) {
            if (smoothed[i] > threshold &&
                smoothed[i] > smoothed[i - 1] &&
                smoothed[i] > smoothed[i + 1] &&
                (i - lastPeak) >= minInterval
            ) {
                val timeSec = (i * hopSize.toFloat() / sampleRate) + timeZeroCorrection
                beatTimes.add(timeSec)
                lastPeak = i
            }
        }

        return beatTimes
    }

}
