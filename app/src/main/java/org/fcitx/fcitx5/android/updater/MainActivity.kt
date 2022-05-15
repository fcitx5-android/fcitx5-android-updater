package org.fcitx.fcitx5.android.updater

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.accompanist.insets.rememberInsetsPaddingValues
import com.google.accompanist.insets.ui.Scaffold
import com.google.accompanist.insets.ui.TopAppBar
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import okhttp3.internal.format
import org.fcitx.fcitx5.android.updater.ui.theme.Fcitx5ForAndroidUpdaterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            Fcitx5ForAndroidUpdaterTheme {
                val systemUiController = rememberSystemUiController()
                systemUiController.setSystemBarsColor(Color.Transparent)
                ProvideWindowInsets {
                    Screen()
                }
            }

        }
    }
}

@Composable
fun Screen() {
    Scaffold(topBar = {
        TopAppBar(
            backgroundColor = MaterialTheme.colors.primarySurface,
            contentPadding = rememberInsetsPaddingValues(
                LocalWindowInsets.current.statusBars,
                applyBottom = false,
            ), title = {
                Text(text = stringResource(R.string.app_name))
            })
    }) {
        VersionList(it)
    }
}


@Composable
fun VersionList(paddingValues: PaddingValues = PaddingValues(0.dp)) {
    val viewModel: MyViewModel = viewModel()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    Box(modifier = Modifier.padding(paddingValues = paddingValues)) {
        SwipeRefresh(state = rememberSwipeRefreshState(isRefreshing = isRefreshing),
            onRefresh = { viewModel.refresh() }) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Versions(
                    stringResource(id = R.string.installed),
                    listOf(viewModel.installedVersion)
                )
                Versions(stringResource(id = R.string.remote), viewModel.remoteVersion)
                Versions(stringResource(id = R.string.local), viewModel.localVersion)
            }
        }
    }

}

@Composable
fun Versions(name: String, versions: List<VersionUi>) {
    VersionListSurface {
        Column {
            Text(
                text = name,
                style = MaterialTheme.typography.h4
            )
            Column {
                for (v in versions)
                    VersionCard(version = v)
            }
        }
    }
}

@Composable
fun VersionListSurface(content: @Composable () -> Unit) {
    Surface(
        color = Color.Transparent,
        modifier = Modifier.padding(16.dp), content = content
    )
}

@Composable
fun VersionCard(version: VersionUi) {
    val viewModel: MyViewModel = viewModel()
    AnimatedVisibility(visible = true) {
        Card(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            elevation = 4.dp,
        ) {
            Box {
                if (version.isInstalled)
                    Icon(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp),
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                    )
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = version.versionName, style = MaterialTheme.typography.h5)
                    Text(
                        text = "${format("%.2f", version.size)} MB",
                        style = MaterialTheme.typography.caption
                    )
                    Divider(modifier = Modifier.padding(top = 10.dp))
                    Row(
                        modifier = Modifier.padding(horizontal = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        when (version) {
                            is VersionUi.Local -> {
                                LocalCardBottomRow(viewModel, version)
                            }
                            is VersionUi.Remote -> {
                                RemoteCardBottomRow(viewModel, version)
                            }
                            is VersionUi.Installed -> {
                                InstalledCardBottomRow(viewModel, version)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RowScope.InstalledCardBottomRow(viewModel: MyViewModel, installed: VersionUi.Installed) {
    if (installed.isInstalled) {
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
            onResult = { viewModel.onUninstall() })
        Button(
            modifier = Modifier.weight(1f),
            elevation = null,
            colors = ButtonDefaults.textButtonColors(),
            onClick = { viewModel.uninstall(launcher) }) {
            Text(text = stringResource(id = R.string.uninstall), color = Color.Red)
        }
    }
}


@Composable
fun RowScope.RemoteCardBottomRow(viewModel: MyViewModel, remote: VersionUi.Remote) {
    val uiState by viewModel.getRemoteUiState(remote).collectAsState()

    @Composable
    fun CancelButton() {
        Button(
            enabled = true,
            modifier = Modifier.weight(1f),
            elevation = null,
            colors = ButtonDefaults.textButtonColors(),
            onClick = { viewModel.cancelDownload(remote) }) {
            Text(text = stringResource(id = R.string.cancel))
        }
    }

    @Composable
    fun ResumeButton() {
        Button(
            enabled = true,
            modifier = Modifier.weight(1f),
            elevation = null,
            colors = ButtonDefaults.textButtonColors(),
            onClick = { viewModel.resumeDownload(remote) }) {
            Text(text = stringResource(id = R.string.resume))
        }
    }

    @Composable
    fun PauseButton() {
        Button(
            enabled = true,
            modifier = Modifier.weight(1f),
            elevation = null,
            colors = ButtonDefaults.textButtonColors(),
            onClick = { viewModel.pauseDownload(remote) }) {
            Text(text = stringResource(id = R.string.pause))
        }
    }

    @Composable
    fun DownloadButton() {
        Button(
            enabled = true,
            modifier = Modifier.weight(1f),
            elevation = null,
            colors = ButtonDefaults.textButtonColors(),
            onClick = { viewModel.download(remote) }) {
            Text(text = stringResource(id = R.string.downloaded))
        }
    }

    @Composable
    fun ProgressBar(progress: Float? = null) {
        if (progress == null)
            LinearProgressIndicator(modifier = Modifier.weight(1f))
        else
            LinearProgressIndicator(
                modifier = Modifier.weight(1f),
                progress = progress
            )
    }
    when (uiState) {
        RemoteUiState.Downloaded -> {
            Text(
                text = stringResource(id = R.string.downloaded),
                modifier = Modifier.weight(1f),
                color = ButtonDefaults.textButtonColors().contentColor(enabled = false).value
            )
        }
        is RemoteUiState.Downloading -> {
            CancelButton()
            ProgressBar((uiState as RemoteUiState.Downloading).progress)
            PauseButton()
        }
        RemoteUiState.Idle -> {
            DownloadButton()
        }
        is RemoteUiState.Pausing -> {
            CancelButton()
            ResumeButton()
            ProgressBar((uiState as RemoteUiState.Pausing).progress)
        }
        RemoteUiState.Pending -> {
            ProgressBar()
        }
    }
}

@Composable
fun RowScope.LocalCardBottomRow(viewModel: MyViewModel, local: VersionUi.Local) {
    Button(
        modifier = Modifier.weight(1f),
        elevation = null,
        colors = ButtonDefaults.textButtonColors(),
        onClick = { viewModel.delete(local) }) {
        Text(text = stringResource(id = R.string.delete_apk), color = Color.Red)
    }
    if (!local.isInstalled)
        Button(
            modifier = Modifier.weight(1f),
            elevation = null,
            colors = ButtonDefaults.textButtonColors(),
            onClick = { viewModel.install(local) }) {
            Text(text = stringResource(id = R.string.install))
        }
}
