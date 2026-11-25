package io.azii

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AudioUploadQueue(
    private val uploader: AudioUploader,
    private val maxQueueBytes: Long = 100L * 1024 * 1024
) {

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default
    )

    private val mutex = Mutex()

    private val pending = mutableListOf<ByteArray>()

    private var pendingBytes = 0L
    private var worker: Job? = null

    // Добавление записи в очередь.
    suspend fun enqueue(wav: ByteArray) {
        require(wav.isNotEmpty()) {
            "Audio is empty"
        }

        // Собственная копия для независимого хранения.
        val audioCopy = wav.copyOf()

        mutex.withLock {
            if (audioCopy.size.toLong() > maxQueueBytes - pendingBytes) {
                audioCopy.fill(0)

                throw IllegalStateException(
                    "RAM upload queue limit reached"
                )
            }

            pending.add(audioCopy)
            pendingBytes += audioCopy.size

            // Запускаем обработчик очереди.
            if (worker?.isActive != true) {
                worker = scope.launch {
                    processQueue()
                }
            }
        }
    }

    // Последовательная отправка записей.
    private suspend fun processQueue() {
        var retryDelay = 1000L

        while (true) {

            val audio = mutex.withLock {
                val item = pending.firstOrNull()

                if (item == null) {
                    worker = null
                }

                item
            } ?: return

            try {
                // Отправляем WAV на VPS.
                uploader.upload(audio)

                // Сервер подтвердил получение.
                mutex.withLock {
                    if (pending.firstOrNull() === audio) {
                        pending.removeAt(0)
                        pendingBytes -= audio.size

                        audio.fill(0)
                    }
                }

                // После успеха возвращаем минимальную задержку.
                retryDelay = 1000L

            } catch (e: CancellationException) {
                throw e

            } catch (e: Exception) {

                // Сохраняем запись в RAM.
                // Повторяем отправку через увеличивающийся интервал.
                delay(retryDelay)

                retryDelay = (retryDelay * 2)
                    .coerceAtMost(60_000L)
            }
        }
    }

    // Сколько записей ожидают отправки.
    suspend fun queuedCount(): Int {
        return mutex.withLock {
            pending.size
        }
    }

    // Общий объём очереди в байтах.
    suspend fun queuedBytes(): Long {
        return mutex.withLock {
            pendingBytes
        }
    }
}