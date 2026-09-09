package com.example.kivo.data.remote

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class AudioRecorder {
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var recordingJob: Job? = null

    companion object {
        private const val SAMPLE_RATE = 44100
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val BUFFER_SIZE_MULTIPLIER = 2
    }

    fun startRecording(onAmplitude: (Float) -> Unit = {}): File? {
        if (isRecording) return null

        val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT) * BUFFER_SIZE_MULTIPLIER
        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) return null

        return try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                audioRecord?.release()
                audioRecord = null
                return null
            }

            isRecording = true
            audioRecord?.startRecording()

            val outputFile = File.createTempFile("kivo_audio_", ".pcm")
            recordingJob = CoroutineScope(Dispatchers.IO).launch {
                val buffer = ShortArray(bufferSize / 2)
                val outputStream = ByteArrayOutputStream()
                val maxAmplitude = Short.MAX_VALUE.toFloat()

                while (isRecording && isActive) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        val bytes = ShortArray(read) { buffer[it] }
                        for (sample in bytes) {
                            outputStream.write(sample.toInt() and 0xFF)
                            outputStream.write((sample.toInt() shr 8) and 0xFF)
                        }
                        val amplitude = bytes.maxOfOrNull { kotlin.math.abs(it.toFloat()) } ?: 0f
                        withContext(Dispatchers.Main) {
                            onAmplitude(amplitude / maxAmplitude)
                        }
                    }
                }

                FileOutputStream(outputFile).use { it.write(outputStream.toByteArray()) }
            }

            outputFile
        } catch (e: Exception) {
            isRecording = false
            audioRecord?.release()
            audioRecord = null
            null
        }
    }

    fun stopRecording(): File? {
        if (!isRecording) return null
        isRecording = false
        recordingJob?.cancel()
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        return null
    }

    fun isRecording(): Boolean = isRecording

    fun release() {
        isRecording = false
        recordingJob?.cancel()
        audioRecord?.release()
        audioRecord = null
    }
}
