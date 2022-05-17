package org.fcitx.fcitx5.android.updater.network

sealed interface DownloadEvent {
    data class Downloading(val progress: Double) : DownloadEvent
    object Purged : DownloadEvent
    object Paused : DownloadEvent
    object Resumed : DownloadEvent
    object Created : DownloadEvent
    object StartCreating : DownloadEvent
    object StartResuming : DownloadEvent
    object StartPurging : DownloadEvent
    object StartPausing : DownloadEvent
    object Downloaded : DownloadEvent
    data class Failed(val cause: Throwable) : DownloadEvent
}