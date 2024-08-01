package org.fcitx.fcitx5.android.updater.network

sealed interface DownloadEvent {
    data class Downloading(val progress: Double) : DownloadEvent
    data object Purged : DownloadEvent
    data object Paused : DownloadEvent
    data object Resumed : DownloadEvent
    data object Created : DownloadEvent
    data object StartCreating : DownloadEvent
    data object StartResuming : DownloadEvent
    data object StartPurging : DownloadEvent
    data object StartPausing : DownloadEvent
    data object Downloaded : DownloadEvent
    data object StartWaitingRetry : DownloadEvent
    data class Failed(val cause: Throwable) : DownloadEvent
}