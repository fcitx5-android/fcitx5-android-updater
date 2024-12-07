package org.fcitx.fcitx5.android.updater.model

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import net.swiftzer.semver.SemVer
import org.fcitx.fcitx5.android.updater.api.CommonApi
import org.fcitx.fcitx5.android.updater.api.JenkinsAndroidJob
import org.fcitx.fcitx5.android.updater.api.JenkinsApi
import org.fcitx.fcitx5.android.updater.bytesToMiB
import org.fcitx.fcitx5.android.updater.extractVersionName
import org.fcitx.fcitx5.android.updater.parallelMap
import org.fcitx.fcitx5.android.updater.parseVersionNumber
import org.fcitx.fcitx5.android.updater.selectByABI

class JenkinsVersionViewModel(private val jenkinsAndroidJob: JenkinsAndroidJob, initialBuildNumbers: List<Int>) :
    VersionViewModel(jenkinsAndroidJob.jobName, jenkinsAndroidJob.pkgName, jenkinsAndroidJob.url) {

    private var buildNumbers = initialBuildNumbers

    override fun refresh() {
        if (isRefreshing.value)
            return
        refreshInstalledVersion()
        allVersions.forEach { (k, v) ->
            val isInstalled = v.versionName == installedVersion.versionName
            allVersions[k] = when (v) {
                is VersionUi.Installed -> v
                is VersionUi.Local -> v.copy(isInstalled = isInstalled)
                is VersionUi.Remote -> v.copy(isInstalled = isInstalled)
            }
        }
        viewModelScope.launch {
            _isRefreshing.emit(true)
            remoteVersions.clear()
            if (hasRefreshed) {
                JenkinsApi.getJobBuilds(jenkinsAndroidJob).also {
                    buildNumbers = it.map { b -> b.buildNumber }
                }
            } else {
                JenkinsApi.getJobBuildsByBuildNumbers(jenkinsAndroidJob, buildNumbers).also {
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
                    -1,
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
            parseVersionNumber(it.versionName).getOrNull()?.let { (a, b, _) ->
                SemVer.parseOrNull("$a-$b")
            }
        }
}
