package io.azii

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var audioRecorder: AudioRecorder
    private lateinit var audioUploader: AudioUploader
    private lateinit var uploadQueue: AudioUploadQueue

    private val uploadScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO
    )

    private var onStartedCallback: (() -> Unit)? = null
    private var onErrorCallback: ((String) -> Unit)? = null

    private val microphonePermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {
                startRecording()
            } else {
                reportError("Нет разрешения на микрофон")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        audioRecorder = AudioRecorder(applicationContext)

        audioUploader = AudioUploader(
            ApiConfig.AUDIO_UPLOAD_URL
        )

        uploadQueue = AudioUploadQueue(
            uploader = audioUploader
        )

        setContent {
            App(
                onStartListening = { onStarted, onError ->

                    if (audioRecorder.isRecording()) {
                        onError("Запись уже выполняется")
                        return@App
                    }

                    onStartedCallback = onStarted
                    onErrorCallback = onError

                    if (
                        checkSelfPermission(
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        startRecording()
                    } else {
                        microphonePermissionLauncher.launch(
                            Manifest.permission.RECORD_AUDIO
                        )
                    }
                },

                onStopListening = {
                    stopRecording()
                }
            )
        }
    }

    // Начало записи.
    private fun startRecording() {
        try {
            audioRecorder.start()

            Log.i("AZII_AUDIO", "Recording started")

            onStartedCallback?.invoke()

            onStartedCallback = null
            onErrorCallback = null

        } catch (e: Exception) {
            reportError(
                e.message ?: "Ошибка запуска микрофона"
            )
        }
    }

    // Остановка записи и автоматическая отправка.
    private fun stopRecording() {
        try {
            val pcm = audioRecorder.stop()

            if (pcm.isEmpty()) {
                Log.w("AZII_AUDIO", "Empty recording")
                return
            }

            // Формируем WAV непосредственно в RAM.
            val wav = try {
                WavEncoder.encode(pcm)
            } finally {
                pcm.fill(0)
            }

            Log.i(
                "AZII_AUDIO",
                "Recording stopped. WAV: ${wav.size} bytes"
            )

            // Добавляем WAV в очередь.
            uploadScope.launch {
                try {
                    uploadQueue.enqueue(wav)

                    Log.i(
                        "AZII_UPLOAD",
                        "WAV added to upload queue"
                    )

                    Log.i(
                        "AZII_UPLOAD",
                        "Pending files: ${uploadQueue.queuedCount()}"
                    )

                } catch (e: Exception) {
                    Log.e(
                        "AZII_UPLOAD",
                        "Cannot enqueue audio",
                        e
                    )
                } finally {
                    // Очередь хранит собственную копию.
                    wav.fill(0)
                }
            }

        } catch (e: Exception) {
            Log.e("AZII_AUDIO", "Recording error", e)
        }
    }

    private fun reportError(message: String) {
        Log.e("AZII_AUDIO", message)

        onErrorCallback?.invoke(message)

        onStartedCallback = null
        onErrorCallback = null
    }

    override fun onDestroy() {
        try {
            audioRecorder.stop().fill(0)
        } catch (_: Exception) {
        }

        super.onDestroy()
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}