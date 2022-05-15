package org.fcitx.fcitx5.android.updater.network

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.Request
import org.fcitx.fcitx5.android.updater.api.CommonApi
import org.fcitx.fcitx5.android.updater.await
import org.fcitx.fcitx5.android.updater.httpClient
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class DownloadTask(
    private val url: String,
    private val file: File,
) :
    CoroutineScope by CoroutineScope(Dispatchers.IO) {

    private val lock = ReentrantLock()

    private val _eventFlow: MutableSharedFlow<DownloadEvent> = MutableSharedFlow()

    val eventFlow = _eventFlow.asSharedFlow()

    var job: Job? = null

    private var created = false

    private var finished = false

    private fun createJob(notify: suspend () -> Unit) {
        job = launch {
            var start = 0L
            if (file.exists())
                start = file.length()
            val request = Request.Builder()
                .addHeader("RANGE", "bytes=$start-")
                .url(url)
                .build()
            notify()
            runCatching {
                val contentLength = CommonApi.getContentLength(url)
                    ?: throw IOException("Failed to get content length")
                if (start == contentLength)
                    return@runCatching
                val response = httpClient.newCall(request).await()
                response.body?.byteStream()?.use {
                    var bytesWritten = 0L
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var bytes = it.read(buffer)
                    val f = RandomAccessFile(file, "rw")
                    f.seek(start)
                    while (isActive && bytes >= 0) {
                        f.write(buffer, 0, bytes)
                        bytesWritten += bytes
                        _eventFlow.emit(DownloadEvent.Progressed(((bytesWritten + start) / contentLength.toDouble())))
                        bytes = it.read(buffer)
                    }
                    f.fd.sync()
                }
            }
                .onSuccess {
                    clear()
                    _eventFlow.emit(DownloadEvent.Downloaded)
                }
                .onFailure {
                    clear()
                    _eventFlow.emit(DownloadEvent.Failed(it))
                }

        }
    }

    private fun clear() {
        job = null
        finished = false
    }

    private fun assertNotFinished() {
        if (finished)
            error("Task is finished")
    }

    fun start() = lock.withLock {
        assertNotFinished()
        if (created || job != null)
            error("Task is already created")
        createJob {
            _eventFlow.emit(DownloadEvent.Created)
            created = true
        }
    }

    fun pause() = lock.withLock {
        assertNotFinished()
        if (!created || job == null)
            error("Task is already paused")
        launch {
            job?.cancelAndJoin()
            job = null
            Log.e("G", "Before emit")
            _eventFlow.emit(DownloadEvent.Paused)
            Log.e("G", "After emit")

        }
    }

    fun resume() = lock.withLock {
        assertNotFinished()
        if (!created || job != null)
            error("Task is not paused")
        createJob {
            _eventFlow.emit(DownloadEvent.Resumed)
        }
    }

    fun purge() = lock.withLock {
        launch {
            job?.cancelAndJoin()
            clear()
            created = false
            if (!finished)
                file.delete()
            _eventFlow.emit(DownloadEvent.Purged)
        }
    }
}