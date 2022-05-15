package org.fcitx.fcitx5.android.updater

sealed interface RemoteUiState {
    data class Downloading(val progress: Float) : RemoteUiState
    data class Pausing(val progress: Float) : RemoteUiState
    object Downloaded : RemoteUiState
    object Pending : RemoteUiState
    object Idle : RemoteUiState
}