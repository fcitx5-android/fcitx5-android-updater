package org.fcitx.fcitx5.android.updater.network

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.Request
import org.fcitx.fcitx5.android.updater.api.CommonApi
import org.fcitx.fcitx5.android.updater.await
import org.fcitx.fcitx5.android.updater.httpClient
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile

// not thread-safe
class DownloadTask(
    private val url: String,
    val file: File,
) :
    CoroutineScope by CoroutineScope(Dispatchers.IO) {

    private val cacheFile = File(file.parent!!, file.nameWithoutExtension + ".$TEMP_EXT")

    private val _eventFlow: MutableSharedFlow<DownloadEvent> = MutableSharedFlow(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val eventFlow = _eventFlow.asSharedFlow()

    var job: Job? = null

    private var created = false

    private var finished = false

    private var contentLength = -1L

    private fun createJob(startEvent: DownloadEvent, notify: suspend () -> Unit) {
        job = launch {
            _eventFlow.emit(startEvent)
            var start = 0L
            if (cacheFile.exists())
                start = cacheFile.length()
            val request = Request.Builder()
                .addHeader("RANGE", "bytes=$start-")
                .url(url)
                .build()
            notify()
            runCatching {
                if (contentLength == -1L)
                    contentLength = CommonApi.getContentLength(url).getOrThrow()
                if (start == contentLength) {
                    finished = true
                    return@runCatching
                }
                val response = httpClient.newCall(request).await()
                response.body?.byteStream()?.use {
                    var bytesWritten = 0L
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var bytes = it.read(buffer)
                    val f = RandomAccessFile(cacheFile, "rw")
                    f.seek(start)
                    while (isActive && bytes >= 0) {
                        f.write(buffer, 0, bytes)
                        bytesWritten += bytes
                        _eventFlow.emit(DownloadEvent.Downloading(((bytesWritten + start) / contentLength.toDouble())))
                        bytes = it.read(buffer)
                    }
                    f.fd.sync()
                    finished = f.length() == contentLength
                } ?: throw IOException("Failed to get body stream")
            }
                .onSuccess {
                    job = null
                    if (finished) {
                        cacheFile.renameTo(file)
                        _eventFlow.emit(DownloadEvent.Downloaded)
                    }
                }
                .onFailure {
                    job = null
                    _eventFlow.emit(DownloadEvent.Failed(it))
                }

        }
    }


    private fun assertNotFinished() {
        if (finished)
            error("Task is finished")
    }

    fun start() {
        assertNotFinished()
        if (created || job != null)
            error("Task is already created")
        createJob(DownloadEvent.StartCreating) {
            _eventFlow.emit(DownloadEvent.Created)
            created = true
        }
    }

    fun pause() {
        assertNotFinished()
        if (!created || job == null)
            error("Task is already paused")
        launch {
            _eventFlow.emit(DownloadEvent.StartPausing)
            job?.cancelAndJoin()
            job = null
            _eventFlow.emit(DownloadEvent.Paused)
        }
    }

    fun resume() {
        assertNotFinished()
        if (!created || job != null)
            error("Task is not paused")
        createJob(DownloadEvent.StartResuming) {
            _eventFlow.emit(DownloadEvent.Resumed)
        }
    }

    fun purge() {
        launch {
            job?.cancelAndJoin()
            job = null
            if (!finished)
                cacheFile.delete()
            else
                file.delete()
            finished = false
            created = false
            _eventFlow.emit(DownloadEvent.Purged)
        }
    }

    companion object {
        const val TEMP_EXT = "tmp"
    }
}