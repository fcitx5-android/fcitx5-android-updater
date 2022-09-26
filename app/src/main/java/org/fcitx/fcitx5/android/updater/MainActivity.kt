package org.fcitx.fcitx5.android.updater

import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import net.swiftzer.semver.SemVer
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
                    val viewModel: MyViewModel = viewModel()
                    val context = LocalContext.current
                    LaunchedEffect(Unit) {
                        viewModel.toastMessage.onEach {
                            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                        }.launchIn(this)
                    }
                    Screen()
                }
            }

        }
    }

    override fun onResume() {
        super.onResume()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onPause() {
        super.onPause()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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
                Versions(
                    stringResource(id = R.string.versions),
                    viewModel.allVersions.values.sortedByDescending {
                        parseVersionNumber(it.versionName).getOrThrow().let { (a, b, _) ->
                            SemVer.parse("$a-$b")
                        }
                    }
                )
            }
        }
    }

}

@Composable
fun Versions(name: String, versions: Iterable<VersionUi>) {
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
                    modifier = Modifier
                        .padding(horizontal = 5.dp)
                        .height(48.dp),
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

@Composable
fun RowScope.InstalledCardBottomRow(viewModel: MyViewModel, installed: VersionUi.Installed) {
    if (installed.isInstalled) {
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
            onResult = { viewModel.refreshIfInstalledChanged() })
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
    fun CancelButton(enable: Boolean) {
        Button(
            enabled = true,
            modifier = Modifier.weight(1f),
            elevation = null,
            colors = ButtonDefaults.textButtonColors(),
            onClick = { if (enable) viewModel.cancelDownload(remote) }) {
            Text(text = stringResource(id = R.string.cancel))
        }
    }

    @Composable
    fun ResumeButton(enable: Boolean) {
        Button(
            enabled = true,
            modifier = Modifier.weight(1f),
            elevation = null,
            colors = ButtonDefaults.textButtonColors(),
            onClick = { if (enable) viewModel.resumeDownload(remote) }) {
            Text(text = stringResource(id = R.string.resume))
        }
    }

    @Composable
    fun PauseButton(enable: Boolean) {
        Button(
            enabled = true,
            modifier = Modifier.weight(1f),
            elevation = null,
            colors = ButtonDefaults.textButtonColors(),
            onClick = { if (enable) viewModel.pauseDownload(remote) }) {
            Text(text = stringResource(id = R.string.pause))
        }
    }

    @Composable
    fun DownloadButton(enable: Boolean) {
        Button(
            enabled = true,
            modifier = Modifier.weight(1f),
            elevation = null,
            colors = ButtonDefaults.textButtonColors(),
            onClick = { if (enable) viewModel.download(remote) }) {
            Text(text = stringResource(id = R.string.download))
        }
    }

    @Composable
    fun ProgressBar(progress: Float? = null) {
        if (progress == null)
            LinearProgressIndicator(modifier = Modifier.weight(1f))
        else {
            LinearProgressIndicator(
                modifier = Modifier.weight(1f),
                progress = progress
            )
        }
    }
    when (uiState) {
        RemoteVersionUiState.Downloaded -> {
            Button(
                enabled = false,
                modifier = Modifier.weight(1f),
                elevation = null,
                colors = ButtonDefaults.textButtonColors(),
                onClick = { }) {
                Text(text = stringResource(id = R.string.downloaded))
            }
        }
        is RemoteVersionUiState.Downloading -> {
            val operable = (uiState as RemoteVersionUiState.Downloading).operable
            CancelButton(operable)
            ProgressBar((uiState as RemoteVersionUiState.Downloading).progress)
            PauseButton(operable)
        }
        is RemoteVersionUiState.Idle -> {
            val operable = (uiState as RemoteVersionUiState.Idle).operable
            DownloadButton(operable)
        }
        is RemoteVersionUiState.Pausing -> {
            val operable = (uiState as RemoteVersionUiState.Pausing).operable
            CancelButton(operable)
            ProgressBar((uiState as RemoteVersionUiState.Pausing).progress)
            ResumeButton(operable)
        }
        RemoteVersionUiState.Pending -> {
            ProgressBar()
        }
        RemoteVersionUiState.WaitingRetry -> {
            Text(text = stringResource(id = R.string.waiting_retry))
            CancelButton(true)
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
    if (!local.isInstalled) {
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
            onResult = { viewModel.refreshIfInstalledChanged() })
        Button(
            modifier = Modifier.weight(1f),
            elevation = null,
            colors = ButtonDefaults.textButtonColors(),
            onClick = { viewModel.install(launcher, local) }) {
            Text(text = stringResource(id = R.string.install))
        }
    }
}
