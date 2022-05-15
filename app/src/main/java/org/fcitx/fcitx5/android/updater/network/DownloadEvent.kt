package org.fcitx.fcitx5.android.updater.network

sealed interface DownloadEvent {
    data class Progressed(val progress: Double) : DownloadEvent
    object Purged : DownloadEvent
    object Paused : DownloadEvent
    object Resumed : DownloadEvent
    object Created : DownloadEvent
    object Downloaded : DownloadEvent
    data class Failed(val cause: Throwable) : DownloadEvent
}