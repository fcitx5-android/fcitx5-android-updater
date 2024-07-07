package org.fcitx.fcitx5.android.updater.api

data class FDroidPackage(
    val pkgName: String,
    val url: String,
    val versions: List<Version>,
) {
    data class Version(
        val versionName: String,
        val artifact: FDroidArtifact,
        val abi: List<String>?,
        val versionCode: Long
    )
}
