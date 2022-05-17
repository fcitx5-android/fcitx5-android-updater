package org.fcitx.fcitx5.android.updater.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.fcitx.fcitx5.android.updater.await
import org.fcitx.fcitx5.android.updater.httpClient

object CommonApi {
    suspend fun getContentLength(url: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .head()
            .build()
        runCatching {
            val response = httpClient.newCall(request).await()
            response.header("Content-Length")?.toLong()
                ?: throw RuntimeException("Unable to get content length")
        }
    }
}