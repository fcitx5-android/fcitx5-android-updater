package org.fcitx.fcitx5.android.updater.ui.components

import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.viewmodel.compose.viewModel
import org.fcitx.fcitx5.android.updater.MainViewModel
import org.fcitx.fcitx5.android.updater.R
import org.fcitx.fcitx5.android.updater.RemoteVersionUiState
import org.fcitx.fcitx5.android.updater.VersionUi

@Composable
fun VersionCardMenuInstalled(version: VersionUi.Installed, dismissMenu: () -> Unit) {
    if (version.isInstalled) {
        val viewModel: MainViewModel = viewModel()
        DropdownMenuItem(
            onClick = {
                dismissMenu()
                viewModel.exportInstalled()
            }
        ) {
            Text(stringResource(R.string.export))
        }
    }
}

@Composable
fun VersionCardMenuLocal(version: VersionUi.Local, dismissMenu: () -> Unit) {
    val viewModel: MainViewModel = viewModel()
    DropdownMenuItem(
        onClick = {
            dismissMenu()
            viewModel.share(version)
        }
    ) {
        Text(stringResource(R.string.share))
    }
    DropdownMenuItem(
        onClick = {
            dismissMenu()
            viewModel.export(version)
        }
    ) {
        Text(stringResource(R.string.export))
    }
    val remoteUrl by remember { mutableStateOf(viewModel.getRemoteUrl(version)) }
    remoteUrl?.let {
        val clipboardManager = LocalClipboardManager.current
        DropdownMenuItem(
            onClick = {
                dismissMenu()
                clipboardManager.setText(AnnotatedString(it))
            }
        ) {
            Text(stringResource(R.string.copy_url))
        }
    }
    DropdownMenuItem(
        onClick = {
            dismissMenu()
            viewModel.delete(version)
        }
    ) {
        Text(stringResource(R.string.delete_apk))
    }
}

@Composable
fun VersionCardMenuRemote(version: VersionUi.Remote, dismissMenu: () -> Unit) {
    val viewModel: MainViewModel = viewModel()
    val state by viewModel.getRemoteUiState(version).collectAsState()
    when (state) {
        RemoteVersionUiState.Downloaded -> {
        }
        is RemoteVersionUiState.Downloading -> {
            DropdownMenuItem(
                onClick = {
                    dismissMenu()
                    viewModel.cancelDownload(version)
                }
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
        is RemoteVersionUiState.Idle -> {
        }
        is RemoteVersionUiState.Pausing -> {
            DropdownMenuItem(
                onClick = {
                    dismissMenu()
                    viewModel.cancelDownload(version)
                }
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
        RemoteVersionUiState.Pending -> {
        }
        RemoteVersionUiState.WaitingRetry -> {
        }
    }
    val clipboardManager = LocalClipboardManager.current
    DropdownMenuItem(
        onClick = {
            dismissMenu()
            clipboardManager.setText(AnnotatedString(version.downloadUrl))
        }
    ) {
        Text(stringResource(R.string.copy_url))
    }
}
