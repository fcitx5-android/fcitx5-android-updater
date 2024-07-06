package org.fcitx.fcitx5.android.updater.model

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import net.swiftzer.semver.SemVer
import org.fcitx.fcitx5.android.updater.VersionUi
import org.fcitx.fcitx5.android.updater.api.AndroidJob
import org.fcitx.fcitx5.android.updater.api.CommonApi
import org.fcitx.fcitx5.android.updater.api.JenkinsApi
import org.fcitx.fcitx5.android.updater.bytesToMiB
import org.fcitx.fcitx5.android.updater.extractVersionName
import org.fcitx.fcitx5.android.updater.parallelMap
import org.fcitx.fcitx5.android.updater.parseVersionNumber
import org.fcitx.fcitx5.android.updater.selectByABI

class JenkinsVersionViewModel(private val androidJob: AndroidJob, initialBuildNumbers: List<Int>) :
    VersionViewModel(androidJob.jobName, androidJob.pkgName, androidJob.url) {

    private var buildNumbers = initialBuildNumbers

    override fun refresh() {
        if (isRefreshing.value)
            return
        viewModelScope.launch {
            _isRefreshing.emit(true)
            remoteVersions.clear()
            if (hasRefreshed) {
                JenkinsApi.getJobBuilds(androidJob).also {
                    buildNumbers = it.map { b -> b.buildNumber }
                }
            } else {
                JenkinsApi.getJobBuildsByBuildNumbers(androidJob, buildNumbers).also {
                    hasRefreshed = true
                }
            }.mapNotNull {
                it.artifacts.selectByABI()?.let { artifact ->
                    artifact.extractVersionName()?.let { versionName ->
                        artifact to versionName
                    }
                }
            }.parallelMap { (artifact, versionName) ->
                VersionUi.Remote(
                    pkgName,
                    versionName,
                    // Bytes to MiB
                    CommonApi.getContentLength(artifact.url)
                        .getOrNull()
                        ?.let { bytesToMiB(it) }
                        ?: .0,
                    versionName == installedVersion.versionName,
                    artifact.url
                )
            }.also {
                allVersions.clear()
                refreshInstalledVersion()
                refreshLocalVersions()
            }.forEach {
                remoteVersions[it.versionName] = it
                if (it.versionName !in localVersions)
                    allVersions[it.versionName] = it
            }
            _isRefreshing.emit(false)
        }
    }

    override val sortedVersions: List<VersionUi>
        get() = allVersions.values.sortedByDescending {
            parseVersionNumber(it.versionName).getOrThrow().let { (a, b, _) ->
                SemVer.parse("$a-$b")
            }
        }
}