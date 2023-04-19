package org.fcitx.fcitx5.android.updater.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.fcitx.fcitx5.android.updater.await
import org.fcitx.fcitx5.android.updater.catResults
import org.fcitx.fcitx5.android.updater.flatMap
import org.fcitx.fcitx5.android.updater.httpClient
import org.fcitx.fcitx5.android.updater.parallelMap
import org.json.JSONObject

object JenkinsApi {

    private suspend fun getAndroidJobs(): Result<List<String>> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://jenkins.fcitx-im.org/job/android/api/json")
            .build()
        runCatching {
            val response = httpClient.newCall(request).await()
            val jObject = JSONObject(response.body.string())
            val jArray = jObject.getJSONArray("jobs")
            val result = mutableListOf<String>()
            for (i in 0 until jArray.length()) {
                val jobObj = jArray.getJSONObject(i)
                val color = jobObj.getString("color")
                if (color == "disabled")
                    continue
                result.add(jobObj.getString("name"))
            }
            result
        }
    }

    private suspend fun getJobBuildNumbersAndDescription(job: String): Result<Pair<List<Int>, String>> =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("https://jenkins.fcitx-im.org/job/android/job/$job/api/json")
                .build()
            runCatching {
                val response = httpClient.newCall(request).await()
                val jObject = JSONObject(response.body.string())
                val jArray = jObject.getJSONArray("builds")
                val numbers = mutableListOf<Int>()
                for (i in 0 until jArray.length()) {
                    numbers.add(jArray.getJSONObject(i).getInt("number"))
                }
                val description = jObject.getString("description")
                numbers to description
            }
        }

    private suspend fun getJobBuild(job: String, buildNumber: Int): Result<JobBuild> =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("https://jenkins.fcitx-im.org/job/android/job/$job/$buildNumber/api/json")
                .build()
            runCatching {
                val response = httpClient.newCall(request).await()
                val jObject = JSONObject(response.body.string())
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
                JobBuild(job, buildNumber, sha1.take(7), artifacts)
            }
        }

    private fun getPackageNameAndUrlFromDescription(description: String): Result<Pair<String, String>> =
        runCatching {
            val jObject = JSONObject(description)
            jObject.getString("pkgName") to jObject.getString("url")
        }

    suspend fun getAllAndroidJobs() =
        getAndroidJobs()
            .getOrElse { emptyList() }
            .parallelMap {
                getJobBuildNumbersAndDescription(it).flatMap { (numbers, description) ->
                    getPackageNameAndUrlFromDescription(description).flatMap { (pkgName, url) ->
                        Result.success(AndroidJob(it, pkgName, url) to numbers)
                    }
                }
            }
            .catResults()
            .associate { it }
            .toSortedMap { a, b -> a.jobName.compareTo(b.jobName) }

    suspend fun getJobBuildsByBuildNumbers(job: AndroidJob, buildNumbers: List<Int>) =
        buildNumbers
            .parallelMap { getJobBuild(job.jobName, it) }
            .catResults()

    suspend fun getJobBuilds(job: AndroidJob) =
        getJobBuildNumbersAndDescription(job.jobName)
            .getOrNull()
            ?.let { getJobBuildsByBuildNumbers(job, it.first) }
            ?: emptyList()
}