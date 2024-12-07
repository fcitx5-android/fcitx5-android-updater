package org.fcitx.fcitx5.android.updater.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.ContentAlpha
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import org.fcitx.fcitx5.android.updater.R
import org.fcitx.fcitx5.android.updater.model.RemoteVersionUiState
import org.fcitx.fcitx5.android.updater.model.VersionUi
import org.fcitx.fcitx5.android.updater.model.VersionViewModel
import org.fcitx.fcitx5.android.updater.versionViewModel

@Composable
fun VersionCardMenu(version: VersionUi, modifier: Modifier) {
    when (version) {
        is VersionUi.Installed -> {
            if (!version.isInstalled) return
            VersionCardMenuIcon(modifier = modifier) { dismissMenu ->
                VersionCardMenuInstalled(version, dismissMenu)
            }
        }
        is VersionUi.Local -> {
            VersionCardMenuIcon(modifier = modifier) { dismissMenu ->
                VersionCardMenuLocal(version, dismissMenu)
            }
        }
        is VersionUi.Remote -> {
            VersionCardMenuIcon(modifier = modifier) { dismissMenu ->
                VersionCardMenuRemote(version, dismissMenu)
            }
        }
    }
}

@Composable
fun VersionCardMenuIcon(
    modifier: Modifier,
    content: @Composable ColumnScope.(dismissMenu: () -> Unit) -> Unit
) {
    Box(modifier = modifier) {
        var menuExpanded by remember { mutableStateOf(false) }
        val dismissMenu = { menuExpanded = false }
        IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(48.dp)) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                tint = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium),
                contentDescription = null
            )
        }
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = dismissMenu,
            modifier = Modifier.defaultMinSize(minWidth = 180.dp),
            content = { content(dismissMenu) }
        )
    }
}

@Composable
fun VersionCardMenuInstalled(version: VersionUi.Installed, dismissMenu: () -> Unit) {
    if (version.isInstalled) {
        val viewModel: VersionViewModel = versionViewModel()
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
    val viewModel: VersionViewModel = versionViewModel()
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
    val viewModel: VersionViewModel = versionViewModel()
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
