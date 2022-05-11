package org.fcitx.fcitx5.android.updater.api

data class WorkflowRun(
    val job: String,
    val buildNumber: Int,
    val revision: String,
    val artifacts: List<Artifact>
)