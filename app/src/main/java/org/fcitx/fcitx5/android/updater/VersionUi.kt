package org.fcitx.fcitx5.android.updater

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.io.File

sealed interface VersionUi : Parcelable {

    val versionName: String

    val size: Double

    val isInstalled: Boolean

    val pkgName: String

    @Parcelize
    data class Installed(
        override val pkgName: String,
        override val versionName: String,
        override val size: Double,
        override val isInstalled: Boolean = true,
    ) : VersionUi

    @Parcelize
    data class Remote(
        override val pkgName: String,
        override val versionName: String,
        override val size: Double,
        override val isInstalled: Boolean,
        val downloadUrl: String,
    ) : VersionUi

    @Parcelize
    data class Local(
        override val pkgName: String,
        override val versionName: String,
        override val size: Double,
        override val isInstalled: Boolean,
        val archiveFile: File,
    ) : VersionUi

    val displayName: String
        get() = "$pkgName-$versionName-${Const.deviceABI}.apk"

    companion object {
        val NotInstalled = Installed("N/A", "N/A", .0, false)
    }
}