package org.fcitx.fcitx5.android.updater.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.fcitx.fcitx5.android.updater.await
import org.fcitx.fcitx5.android.updater.httpClient

object GitHubApi {

    suspend fun getCommitNumber(owner: String, repo: String, gitHash: String): Result<String> =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("https://api.github.com/repos/$owner/$repo/commits?per_page=1&sha=$gitHash")
                .header("User-Agent", "request")
                .head()
                .build()
            runCatching {
                val response =
                    httpClient.newCall(request).await()
                val links = response.header("Link")
                checkNotNull(links) { "Unable to find 'Link' in headers" }
                val result = REGEX.find(links)
                result?.groupValues?.getOrNull(1) ?: error("Failed to parse $links")
            }
        }

    private val REGEX = "next.*page=(\\d+).*last.*".toRegex()
}