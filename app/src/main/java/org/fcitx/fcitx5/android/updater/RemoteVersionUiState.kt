package org.fcitx.fcitx5.android.updater

sealed interface RemoteVersionUiState {
    data class Downloading(val operable: Boolean, val progress: Float) : RemoteVersionUiState
    data class Pausing(val operable: Boolean, val progress: Float) : RemoteVersionUiState
    object Downloaded : RemoteVersionUiState
    object Pending : RemoteVersionUiState
    data class Idle(val operable: Boolean) : RemoteVersionUiState
    object WaitingRetry : RemoteVersionUiState
}