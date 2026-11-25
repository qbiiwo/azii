package io.azii

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.io.ByteArrayOutputStream

class AudioRecorder(context: Context) {

    private val appContext = context.applicationContext

    companion object {
        const val SAMPLE_RATE = 16000
        const val MAX_RECORDING_SIZE = 25 * 1024 * 1024
    }

    @Volatile
    private var recording = false

    @Volatile
    private var recordingError: String? = null

    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    private var audioBuffer: ByteArrayOutputStream? = null

    fun start() {
        if (audioRecord != null) {
            throw IllegalStateException("Recorder already initialized")
        }

        if (
            appContext.checkSelfPermission(Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            throw SecurityException("Microphone permission not granted")
        }

        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        if (minBufferSize <= 0) {
            throw IllegalStateException("Invalid audio buffer size")
        }

        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBufferSize, 8192)
        )

        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            throw IllegalStateException("Cannot initialize microphone")
        }

        audioBuffer = ByteArrayOutputStream()
        audioRecord = recorder
        recordingError = null

        try {
            recorder.startRecording()
        } catch (e: Exception) {
            recorder.release()
            audioRecord = null
            audioBuffer = null
            throw e
        }

        recording = true

        recordingThread = Thread {
            val chunk = ByteArray(4096)

            try {
                while (recording) {
                    val remaining =
                        MAX_RECORDING_SIZE - (audioBuffer?.size() ?: 0)

                    if (remaining <= 0) {
                        recording = false
                        break
                    }

                    val count = recorder.read(
                        chunk,
                        0,
                        minOf(chunk.size, remaining)
                    )

                    if (count > 0) {
                        audioBuffer?.write(chunk, 0, count)
                    } else if (count < 0) {
                        recordingError = "AudioRecord error: $count"
                        recording = false
                    }
                }
            } catch (e: Exception) {
                recordingError = e.message ?: "Recording error"
                recording = false
            }
        }.apply {
            name = "AZII-AudioRecorder"
            start()
        }
    }

    fun stop(): ByteArray {
        val recorder = audioRecord ?: return ByteArray(0)

        recording = false

        try {
            if (recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                recorder.stop()
            }
        } catch (_: IllegalStateException) {
            // Recorder may already have stopped.
        }

        recordingThread?.join()

        recorder.release()

        audioRecord = null
        recordingThread = null

        val result = audioBuffer?.toByteArray() ?: ByteArray(0)

        audioBuffer?.reset()
        audioBuffer = null

        recordingError?.let {
            recordingError = null
            throw IllegalStateException(it)
        }

        return result
    }

    fun isRecording(): Boolean {
        return recording
    }
}