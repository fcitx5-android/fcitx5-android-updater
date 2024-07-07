package org.fcitx.fcitx5.android.updater.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.fcitx.fcitx5.android.updater.await
import org.fcitx.fcitx5.android.updater.bytesToMiB
import org.fcitx.fcitx5.android.updater.httpClient
import org.json.JSONObject

object FDroidApi {
    private suspend fun getAll() = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://f5a.torus.icu/fdroid/repo/index-v2.json")
            .build()
        runCatching {
            val response = httpClient.newCall(request).await()
            val jObject = JSONObject(response.body.string())
            val packages = jObject.getJSONObject("packages")
            val fDroidPackages = mutableListOf<FDroidPackage>()
            packages.keys().forEach { pkgName ->
                fDroidPackages.add(parsePackage(pkgName, packages.getJSONObject(pkgName)))
            }
            fDroidPackages
        }
    }

    private fun parsePackage(pkgName: String, packageObj: JSONObject): FDroidPackage {
        val metadataObj = packageObj.getJSONObject("metadata")
        val url = metadataObj.getString("webSite")
        val versionsObj = packageObj.getJSONObject("versions")
        val versions = mutableListOf<FDroidPackage.Version>()
        versionsObj.keys().forEach { sha ->
            val versionObj = versionsObj.getJSONObject(sha)
            val version = parseVersion(versionObj)
            versions.add(version)
        }
        return FDroidPackage(pkgName, url, versions)
    }

    private fun parseVersion(versionObj: JSONObject): FDroidPackage.Version {
        val added = versionObj.getLong("added")
        val fileObj = versionObj.getJSONObject("file")
        // drop prefix /
        val fileName = fileObj.getString("name").drop(1)
        val fileSize = fileObj.getString("size").toLong()
        val manifestObj = versionObj.getJSONObject("manifest")
        val versionName = manifestObj.getString("versionName")
        val abi = manifestObj.optJSONArray("nativecode")
        val abiList = abi?.let {
            val list = mutableListOf<String>()
            for (i in 0..<abi.length()) {
                list.add(it.getString(i))
            }
            list
        }
        return FDroidPackage.Version(
            versionName,
            FDroidArtifact(
                bytesToMiB(fileSize),
                fileName,
                "https://f5a.torus.icu/fdroid/repo/$fileName"
            ), abiList, added
        )
    }

    private suspend fun getPackage(pkgName: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://f5a.torus.icu/fdroid/repo/index-v2.json")
            .build()
        runCatching {
            val response = httpClient.newCall(request).await()
            val jObject = JSONObject(response.body.string())
            val packages = jObject.getJSONObject("packages")
            val pkg = packages.getJSONObject(pkgName)
            parsePackage(pkgName, pkg)
        }
    }


    suspend fun getAllPackages() = getAll().getOrElse { emptyList() }

    suspend fun getPackageVersions(pkgName: String) =
        getPackage(pkgName).map { it.versions }.getOrElse { emptyList() }
}