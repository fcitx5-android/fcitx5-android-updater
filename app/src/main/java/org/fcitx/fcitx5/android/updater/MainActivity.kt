package org.fcitx.fcitx5.android.updater

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.insets.ui.Scaffold
import com.google.accompanist.insets.ui.TopAppBar
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import net.swiftzer.semver.SemVer
import org.fcitx.fcitx5.android.updater.api.JenkinsApi
import org.fcitx.fcitx5.android.updater.ui.components.Versions
import org.fcitx.fcitx5.android.updater.ui.theme.Fcitx5ForAndroidUpdaterTheme
import java.io.File

class MainActivity : ComponentActivity() {


    private val viewModels = mutableMapOf<String, VersionViewModel>()

    private lateinit var intentLauncher: ActivityResultLauncher<Intent>

    private lateinit var exportLauncher: ActivityResultLauncher<String>
    private lateinit var exportFile: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        intentLauncher = registerForActivityResult(StartActivityForResult()) {
            viewModels.forEach { it.value.refreshIfInstalledChanged() }
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
        setContent {
            Fcitx5ForAndroidUpdaterTheme {
                SplashScreen()
            }
        }

        lifecycleScope.launch {
            val androidJobs = JenkinsApi.getAllAndroidJobs()
            androidJobs.forEach { job ->
                val viewModel = VersionViewModel(job)
                viewModel.toastMessage.onEach {
                    Toast.makeText(this@MainActivity, "${job.jobName}: $it", Toast.LENGTH_SHORT)
                        .show()
                }.launchIn(this)
                viewModel.fileOperation.onEach {
                    when (it) {
                        is FileOperation.Install -> {
                            intentLauncher.launch(PackageUtils.installIntent(it.file))
                        }
                        is FileOperation.Uninstall -> {
                            intentLauncher.launch(PackageUtils.uninstallIntent(it.packageName))
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
                viewModels[job.jobName] = viewModel
            }
            setContent {
                val systemUiController = rememberSystemUiController()
                Fcitx5ForAndroidUpdaterTheme {
                    val useDarkIcons = MaterialTheme.colors.isLight
                    SideEffect {
                        systemUiController.setStatusBarColor(Color.Transparent)
                        systemUiController.setNavigationBarColor(
                            Color.Transparent,
                            useDarkIcons
                        )
                    }
                    MainScreen(viewModels)
                }
            }
        }
    }
}

@Composable
fun MainScreen(viewModels: Map<String, VersionViewModel>) {
    val navController = rememberNavController()
    val scaffoldState = rememberScaffoldState()
    val scope = rememberCoroutineScope()
    Scaffold(
        scaffoldState = scaffoldState,
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal)),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                backgroundColor = MaterialTheme.colors.primarySurface,
                contentPadding = WindowInsets.statusBars.asPaddingValues(),
                navigationIcon = {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Drawer Icon",
                        Modifier.clickable {
                            scope.launch {
                                scaffoldState.drawerState.open()
                            }
                        }
                    )
                }
            )
        },
        drawerContent = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(60.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                viewModels.forEach { (jobName, _) ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch {
                                    scaffoldState.drawerState.close()
                                }
                                navController.navigate(jobName)
                            },
                        elevation = 0.dp,
                    ) {
                        Text(text = jobName)
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "fcitx5-android",
            modifier = Modifier.padding(paddingValues)
        ) {
            viewModels.forEach { (jobName, viewModel) ->
                composable(jobName) {
                    VersionScreen(viewModel)
                }
            }
        }
    }
}

@Composable
fun SplashScreen() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(text = "Loading...", style = MaterialTheme.typography.h2)
    }
}


val LocalVersionViewModel = compositionLocalOf<VersionViewModel> { error("No view model") }

@Composable
fun versionViewModel() = LocalVersionViewModel.current

@Composable
fun VersionScreen(viewModel: VersionViewModel) {
    CompositionLocalProvider(LocalVersionViewModel provides viewModel) {
        val isRefreshing by viewModel.isRefreshing.collectAsState()
        SwipeRefresh(
            state = rememberSwipeRefreshState(isRefreshing = isRefreshing),
            onRefresh = { viewModel.refresh() }
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = viewModel.androidJob.jobName,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.h5
                )
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
