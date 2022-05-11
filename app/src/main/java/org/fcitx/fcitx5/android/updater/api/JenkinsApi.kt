package org.fcitx.fcitx5.android.updater.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.fcitx.fcitx5.android.updater.await
import org.fcitx.fcitx5.android.updater.parallelMap
import org.json.JSONObject

object JenkinsApi {

    private val client = OkHttpClient()

    private suspend fun getJobBuildNumbers(job: String): Result<List<Int>> =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("https://jenkins.fcitx-im.org/job/android/job/$job/api/json")
                .build()
            val response = client.newCall(request).await()
            runCatching {
                val jObject = JSONObject(response.body!!.string())
                val jArray = jObject.getJSONArray("builds")
                val result = mutableListOf<Int>()
                for (i in 0 until jArray.length()) {
                    result.add(jArray.getJSONObject(i).getInt("number"))
                }
                result
            }
        }

    private suspend fun getWorkflowRun(job: String, buildNumber: Int): Result<WorkflowRun> =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("https://jenkins.fcitx-im.org/job/android/job/$job/$buildNumber/api/json")
                .build()
            val response = client.newCall(request).await()
            runCatching {
                val jObject = JSONObject(response.body!!.string())
                val actions = jObject.getJSONArray("actions")
                var buildData: JSONObject? = null
                for (i in 0 until actions.length()) {
                    val action = actions.getJSONObject(i)
                    if (action.optString("_class") == "hudson.plugins.git.util.BuildData") {
                        buildData = action
                        break
                    }
                }
                requireNotNull(buildData) { "Failed to find buildData" }
                val sha1 = buildData.getJSONObject("lastBuiltRevision").getString("SHA1")
                val artifacts = mutableListOf<Artifact>()
                val artifactArray = jObject.getJSONArray("artifacts")
                for (i in 0 until artifactArray.length()) {
                    val artifact = artifactArray.getJSONObject(i)
                    artifacts.add(
                        Artifact(
                            artifact.getString("fileName"),
                            artifact.getString("relativePath"),
                            "https://jenkins.fcitx-im.org/job/android/job/$job/$buildNumber/artifact/${
                                artifact.getString(
                                    "relativePath"
                                )
                            }"
                        )
                    )
                }
                WorkflowRun(job, buildNumber, sha1.take(7), artifacts)
            }
        }

    suspend fun getAllWorkflowRuns(job: String) =
        getJobBuildNumbers(job)
            .getOrDefault(listOf())
            .parallelMap { getWorkflowRun(job, it) }

    suspend fun getArtifactSize(artifact: Artifact) = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(artifact.url)
            .head()
            .build()
        val response = client.newCall(request).await()
        response.header("Content-Length")?.toIntOrNull()?.let { it / 1E6 }
    }
}