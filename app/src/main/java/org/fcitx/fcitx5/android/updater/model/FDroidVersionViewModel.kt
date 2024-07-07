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
    override fun refresh() {
        if (isRefreshing.value)
            return
        viewModelScope.launch {
            _isRefreshing.emit(true)
            remoteVersions.clear()
            FDroidApi.getPackageVersions(pkg.pkgName)
                .mapNotNull {
                    if (it.abi?.contains(Const.deviceABI) != false) {
                        VersionUi.Remote(
                            pkgName,
                            it.versionCode,
                            it.versionName,
                            it.artifact.size,
                            it.versionCode == installedVersion.versionCode,
                            it.artifact.url,
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
        get() = allVersions.values.sortedByDescending { it.versionCode }
}