package org.fcitx.fcitx5.android.updater

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.fcitx.fcitx5.android.updater.api.Artifact
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun Call.await() = suspendCancellableCoroutine<Response> {
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            it.resumeWithException(e)
        }

        override fun onResponse(call: Call, response: Response) {
            it.resume(response)
        }
    })
    it.invokeOnCancellation {
        runCatching { cancel() }
    }
}

suspend fun <A, B> Iterable<A>.parallelMap(f: suspend (A) -> B): List<B> = coroutineScope {
    map { async { f(it) } }.awaitAll()
}

fun List<Artifact>.selectByABI() = find { it.fileName.contains(PackageUtils.deviceABI) }