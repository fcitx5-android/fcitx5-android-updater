package org.fcitx.fcitx5.android.updater.model

sealed interface RemoteVersionUiState {
    data class Downloading(val operable: Boolean, val progress: Float) : RemoteVersionUiState
    data class Pausing(val operable: Boolean, val progress: Float) : RemoteVersionUiState
    data object Downloaded : RemoteVersionUiState
    data object Pending : RemoteVersionUiState
    data class Idle(val operable: Boolean) : RemoteVersionUiState
    data object WaitingRetry : RemoteVersionUiState
}