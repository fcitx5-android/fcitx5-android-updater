package org.fcitx.fcitx5.android.updater.ui.components

import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.viewmodel.compose.viewModel
import org.fcitx.fcitx5.android.updater.MainViewModel
import org.fcitx.fcitx5.android.updater.R
import org.fcitx.fcitx5.android.updater.RemoteVersionUiState
import org.fcitx.fcitx5.android.updater.VersionUi

@Composable
fun VersionCardMenuInstalled(version: VersionUi.Installed, dismissMenu: () -> Unit) {
    val viewModel: MainViewModel = viewModel()
    DropdownMenuItem(onClick = { dismissMenu() }) {
        Text(stringResource(R.string.share))
    }
    DropdownMenuItem(onClick = { dismissMenu() }) {
        Text(stringResource(R.string.export))
    }
}

@Composable
fun VersionCardMenuLocal(version: VersionUi.Local, dismissMenu: () -> Unit) {
    val viewModel: MainViewModel = viewModel()
    DropdownMenuItem(onClick = { dismissMenu() }) {
        Text(stringResource(R.string.share))
    }
    DropdownMenuItem(onClick = { dismissMenu() }) {
        Text(stringResource(R.string.export))
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
