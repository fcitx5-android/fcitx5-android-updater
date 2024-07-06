package org.fcitx.fcitx5.android.updater.api

data class JenkinsJobBuild(
    val jobName: String,
    val buildNumber: Int,
    val revision: String,
    val artifacts: List<JenkinsArtifact>
)