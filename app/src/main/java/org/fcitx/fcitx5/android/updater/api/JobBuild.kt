package org.fcitx.fcitx5.android.updater.api

data class JobBuild(
    val jobName: String,
    val buildNumber: Int,
    val revision: String,
    val artifacts: List<Artifact>
)