package org.fcitx.fcitx5.android.updater

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
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

fun List<Artifact>.selectByABI() =
    find { it.fileName.endsWith(".apk") && it.fileName.contains(Const.deviceABI) }

fun Artifact.extractVersionName() = artifactNameRegex.find(fileName)?.let {
    val groups = it.groupValues
    groups.getOrNull(1)?.let { tag ->
        groups.getOrNull(2)?.toIntOrNull()?.let { commitInc ->
            groups.getOrNull(3)?.let { hash ->
                "$tag-$commitInc-g$hash"
            }
        }
    }
}

private val artifactNameRegex = "\\S*-([^-]+)-([^-]+)-g([^-]+)-\\S*".toRegex()


fun parseVersionNumber(raw: String): Result<Triple<String, Int, String>> = runCatching {
    val g = raw.split('-')
    require(g.size == 3)
    val tag = g[0]
    val commitInc = g[1].toInt()
    val hash = g[2].drop(1)
    Triple(tag, commitInc, hash)
}

val httpClient = OkHttpClient()

val externalDir by lazy { MyApplication.context.getExternalFilesDir(null)!! }
