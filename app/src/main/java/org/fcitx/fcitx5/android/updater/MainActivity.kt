package org.fcitx.fcitx5.android.updater

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.insets.ui.Scaffold
import com.google.accompanist.insets.ui.TopAppBar
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import net.swiftzer.semver.SemVer
import org.fcitx.fcitx5.android.updater.ui.components.VersionCard
import org.fcitx.fcitx5.android.updater.ui.theme.Fcitx5ForAndroidUpdaterTheme
import java.io.File

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private lateinit var intentLauncher: ActivityResultLauncher<Intent>

    private lateinit var exportLauncher: ActivityResultLauncher<String>
    private lateinit var exportFile: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        intentLauncher = registerForActivityResult(StartActivityForResult()) {
            viewModel.refreshIfInstalledChanged()
        }
        exportLauncher = registerForActivityResult(CreateDocument(Const.apkMineType)) {
            it?.let { uri ->
                lifecycleScope.launch {
                    contentResolver.openOutputStream(uri)?.use { o ->
                        exportFile.inputStream().use { i ->
                            i.copyTo(o)
                        }
                    }
                }
            }
        }
        lifecycleScope.launch {
            viewModel.toastMessage.onEach {
                Toast.makeText(this@MainActivity, it, Toast.LENGTH_SHORT).show()
            }.launchIn(this)
            viewModel.fileOperation.onEach {
                when (it) {
                    is FileOperation.Install -> {
                        intentLauncher.launch(PackageUtils.installIntent(it.file))
                    }
                    FileOperation.Uninstall -> {
                        intentLauncher.launch(PackageUtils.uninstallIntent())
                    }
                    is FileOperation.Share -> {
                        val shareIntent = PackageUtils.shareIntent(it.file, it.name)
                        startActivity(Intent.createChooser(shareIntent, it.name))
                    }
                    is FileOperation.Export -> {
                        exportFile = it.file
                        exportLauncher.launch(it.name)
                    }
                    // TODO: share installed apk with FileProvider
                }
            }.launchIn(this)
        }
        setContent {
            val systemUiController = rememberSystemUiController()
            Fcitx5ForAndroidUpdaterTheme {
                val useDarkIcons = MaterialTheme.colors.isLight
                SideEffect {
                    systemUiController.setStatusBarColor(Color.Transparent)
                    systemUiController.setNavigationBarColor(Color.Transparent, useDarkIcons)
                }
                Screen()
            }
        }
    }
}

@Composable
fun Screen() {
    val viewModel: MainViewModel = viewModel()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    Scaffold(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal)),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                backgroundColor = MaterialTheme.colors.primarySurface,
                contentPadding = WindowInsets.statusBars.asPaddingValues()
            )
        }
    ) { paddingValues ->
        SwipeRefresh(
            state = rememberSwipeRefreshState(isRefreshing = isRefreshing),
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.padding(paddingValues)
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Versions(
                    stringResource(R.string.installed),
                    listOf(viewModel.installedVersion)
                )
                Versions(
                    stringResource(R.string.versions),
                    viewModel.allVersions.values.sortedByDescending {
                        parseVersionNumber(it.versionName).getOrThrow().let { (a, b, _) ->
                            SemVer.parse("$a-$b")
                        }
                    }
                )
                Spacer(
                    Modifier
                        .padding(bottom = 16.dp)
                        .windowInsetsBottomHeight(WindowInsets.navigationBars)
                )
            }
        }
    }
}

@Composable
fun Versions(name: String, versions: Iterable<VersionUi>) {
    Column(Modifier.padding(vertical = 8.dp)) {
        Text(
            text = name,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
            style = MaterialTheme.typography.h6
        )
        Surface(elevation = 4.dp) {
            Column {
                for (v in versions)
                    VersionCard(version = v)
            }
        }
    }
}
