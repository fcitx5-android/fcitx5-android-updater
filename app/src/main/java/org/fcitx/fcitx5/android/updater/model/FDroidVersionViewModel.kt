package org.fcitx.fcitx5.android.updater.model

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.updater.Const
import org.fcitx.fcitx5.android.updater.api.FDroidApi
import org.fcitx.fcitx5.android.updater.api.FDroidPackage

class FDroidVersionViewModel(private val pkg: FDroidPackage) : VersionViewModel(
    pkg.pkgName.removePrefix("org.fcitx.fcitx5.android.plugin.").replace('_', '-'),
    pkg.pkgName,
    pkg.url
) {
    private val versionTimestamp = mutableMapOf<String, Long>()
    override fun refresh() {
        if (isRefreshing.value)
            return
        viewModelScope.launch {
            _isRefreshing.emit(true)
            remoteVersions.clear()
            versionTimestamp.clear()
            FDroidApi.getPackageVersions(pkg.pkgName)
                .mapNotNull {
                    if (it.abi?.contains(Const.deviceABI) != false) {
                        versionTimestamp[it.versionName] = it.added
                        VersionUi.Remote(
                            pkgName,
                            it.versionName,
                            it.artifact.size,
                            it.versionName == installedVersion.versionName,
                            it.artifact.url
                        )
                    } else null
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
        get() = allVersions.values.sortedByDescending { versionTimestamp[it.versionName] }
}