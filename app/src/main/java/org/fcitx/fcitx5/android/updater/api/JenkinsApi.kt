package org.fcitx.fcitx5.android.updater.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.fcitx.fcitx5.android.updater.*
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

    private fun getPackageNameFromDescription(description: String): Result<String> = runCatching {
        val jObject = JSONObject(description)
        jObject.getString("pkgName")
    }

    suspend fun getAllAndroidJobs() =
        getAndroidJobs()
            .getOrElse { emptyList() }
            .parallelMap {
                getJobBuildNumbersAndDescription(it).flatMap { (numbers, description) ->
                    getPackageNameFromDescription(description).flatMap { pkgName ->
                        Result.success(AndroidJob(it, pkgName, numbers))
                    }
                }
            }
            .catResults()

    suspend fun getJobBuilds(job: AndroidJob) =
        job.buildNumbers
            .parallelMap { getJobBuild(job.jobName, it) }
            .catResults()
}