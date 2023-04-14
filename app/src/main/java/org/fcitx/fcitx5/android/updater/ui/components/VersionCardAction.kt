package org.fcitx.fcitx5.android.updater.ui.components

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.viewmodel.compose.viewModel
import org.fcitx.fcitx5.android.updater.R
import org.fcitx.fcitx5.android.updater.RemoteVersionUiState
import org.fcitx.fcitx5.android.updater.VersionUi
import org.fcitx.fcitx5.android.updater.VersionViewModel

@Composable
fun VersionCardAction(
    version: VersionUi,
    modifier: Modifier
) {
    when (version) {
        is VersionUi.Installed -> VersionCardActionInstalled(version, modifier)
        is VersionUi.Local -> VersionCardActionLocal(version, modifier)
        is VersionUi.Remote -> VersionCardActionRemote(version, modifier)
    }
}

@Composable
fun VersionCardActionInstalled(
    version: VersionUi.Installed,
    modifier: Modifier
) {
    val viewModel: VersionViewModel = viewModel()
    if (version.isInstalled) {
        ConstraintLayout(modifier = modifier) {
            val (action) = createRefs()
            TextButton(
                onClick = { viewModel.uninstall() },
                modifier = Modifier.constrainAs(action) {
                    top.linkTo(parent.top)
                    end.linkTo(parent.end)
                },
                content = { Text(text = stringResource(R.string.uninstall)) }
            )
        }
    }
}

@Composable
fun VersionCardActionLocal(
    version: VersionUi.Local,
    modifier: Modifier
) {
    val viewModel: VersionViewModel = viewModel()
    ConstraintLayout(modifier = modifier) {
        val (action) = createRefs()
        TextButton(
            onClick = { viewModel.install(version) },
            modifier = Modifier.constrainAs(action) {
                top.linkTo(parent.top)
                end.linkTo(parent.end)
            },
            content = { Text(stringResource(R.string.install)) }
        )
    }
}

@Composable
fun VersionCardActionRemote(
    version: VersionUi.Remote,
    modifier: Modifier
) {
    val viewModel: VersionViewModel = viewModel()
    val state by viewModel.getRemoteUiState(version).collectAsState()
    ConstraintLayout(modifier = modifier) {
        val (button, progressText, progressBar) = createRefs()
        val buttonModifier = Modifier
            .defaultMinSize(minWidth = 72.dp)
            .constrainAs(button) {
                top.linkTo(parent.top)
                end.linkTo(parent.end)
                bottom.linkTo(parent.bottom)
            }
        val progressTextModifier = Modifier.constrainAs(progressText) {
            width = Dimension.value(36.dp)
            top.linkTo(parent.top)
            end.linkTo(button.start, 8.dp)
            bottom.linkTo(parent.bottom)
        }
        val progressBarModifier = Modifier.constrainAs(progressBar) {
            width = Dimension.fillToConstraints
            top.linkTo(parent.top)
            start.linkTo(parent.start)
            end.linkTo(progressText.start, 8.dp)
            bottom.linkTo(parent.bottom)
        }
        when (state) {
            RemoteVersionUiState.Downloaded -> {
                TextButton(
                    onClick = { },
                    modifier = buttonModifier,
                    enabled = false,
                    content = { Text(text = stringResource(R.string.downloaded)) }
                )
            }
            is RemoteVersionUiState.Downloading -> {
                val operable = (state as RemoteVersionUiState.Downloading).operable
                TextButton(
                    onClick = { if (operable) viewModel.pauseDownload(version) },
                    modifier = buttonModifier,
                    content = { Text(text = stringResource(R.string.pause)) }
                )
                val progress = (state as RemoteVersionUiState.Downloading).progress
                CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        modifier = progressTextModifier,
                        textAlign = TextAlign.End,
                        style = MaterialTheme.typography.body2
                    )
                }
                LinearProgressIndicator(progress = progress, modifier = progressBarModifier)
            }
            is RemoteVersionUiState.Idle -> {
                val operable = (state as RemoteVersionUiState.Idle).operable
                TextButton(
                    onClick = { if (operable) viewModel.download(version) },
                    modifier = buttonModifier,
                    content = { Text(text = stringResource(R.string.download)) }
                )
            }
            is RemoteVersionUiState.Pausing -> {
                val operable = (state as RemoteVersionUiState.Pausing).operable
                TextButton(
                    onClick = { if (operable) viewModel.resumeDownload(version) },
                    modifier = buttonModifier,
                    content = { Text(text = stringResource(R.string.resume)) }
                )
                val progress = (state as RemoteVersionUiState.Pausing).progress
                CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        modifier = progressTextModifier,
                        textAlign = TextAlign.End,
                        style = MaterialTheme.typography.body2
                    )
                }
                LinearProgressIndicator(progress = progress, modifier = progressBarModifier)
            }
            RemoteVersionUiState.Pending -> {
                TextButton(
                    onClick = { viewModel.cancelDownload(version) },
                    modifier = buttonModifier,
                    content = { Text(text = stringResource(R.string.cancel)) }
                )
                Text(
                    text = "",
                    modifier = progressTextModifier,
                    style = MaterialTheme.typography.body2
                )
                LinearProgressIndicator(modifier = progressBarModifier)
            }
            RemoteVersionUiState.WaitingRetry -> {
                TextButton(
                    onClick = { viewModel.cancelDownload(version) },
                    modifier = buttonModifier,
                    content = { Text(text = stringResource(R.string.cancel)) }
                )
                Text(
                    text = "",
                    modifier = progressTextModifier,
                    style = MaterialTheme.typography.body2
                )
                LinearProgressIndicator(modifier = progressBarModifier)
            }
        }
    }
}
