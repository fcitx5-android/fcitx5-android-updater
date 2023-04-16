package org.fcitx.fcitx5.android.updater.api

data class AndroidJob(
    val jobName: String,
    val pkgName: String,
    val buildNumbers: List<Int>,
    val url: String
)
