package org.fcitx.fcitx5.android.updater.utils

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

val httpClient = OkHttpClient()

suspend fun Call.await() = suspendCancellableCoroutine {
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
