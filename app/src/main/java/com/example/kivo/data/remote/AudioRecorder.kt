package com.example.kivo.data.remote

import android.media.MediaRecorder
import android.os.Build
import kotlinx.coroutines.*
import java.io.File

class AudioRecorder {
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var outputFile: File? = null
    private var amplitudeJob: Job? = null

    fun startRecording(onAmplitude: (Float) -> Unit = {}): File? {
        if (isRecording) return null

        return try {
            outputFile = File.createTempFile("kivo_audio_", ".m4a")

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder()
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128000)
                setMaxDuration(300000) // 5 minutos max
                setOutputFile(outputFile?.absolutePath)
                prepare()
                start()
            }

            isRecording = true

            amplitudeJob = CoroutineScope(Dispatchers.IO).launch {
                while (isRecording && isActive) {
                    try {
                        val amp = mediaRecorder?.maxAmplitude ?: 0
                        val normalized = (amp.toFloat() / 32767f).coerceIn(0f, 1f)
                        withContext(Dispatchers.Main) {
                            onAmplitude(normalized)
                        }
                    } catch (_: Exception) {}
                    delay(100)
                }
            }

            outputFile
        } catch (e: Exception) {
            isRecording = false
            mediaRecorder?.release()
            mediaRecorder = null
            null
        }
    }

    fun stopRecording(): File? {
        if (!isRecording) return null
        isRecording = false
        amplitudeJob?.cancel()
        return try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
            mediaRecorder = null
            outputFile
        } catch (e: Exception) {
            mediaRecorder?.release()
            mediaRecorder = null
            null
        }
    }

    fun isRecording(): Boolean = isRecording

    fun release() {
        isRecording = false
        amplitudeJob?.cancel()
        mediaRecorder?.release()
        mediaRecorder = null
    }
}
