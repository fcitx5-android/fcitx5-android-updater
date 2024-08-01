package org.fcitx.fcitx5.android.updater

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.DrawerDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.ListItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.primarySurface
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.updater.api.FDroidApi
import org.fcitx.fcitx5.android.updater.api.JenkinsApi
import org.fcitx.fcitx5.android.updater.model.FDroidVersionViewModel
import org.fcitx.fcitx5.android.updater.model.FileOperation
import org.fcitx.fcitx5.android.updater.model.JenkinsVersionViewModel
import org.fcitx.fcitx5.android.updater.model.MainViewModel
import org.fcitx.fcitx5.android.updater.model.VersionViewModel
import org.fcitx.fcitx5.android.updater.ui.components.Versions
import org.fcitx.fcitx5.android.updater.ui.theme.Fcitx5ForAndroidUpdaterTheme
import java.io.File

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private lateinit var intentLauncher: ActivityResultLauncher<Intent>

    private lateinit var exportLauncher: ActivityResultLauncher<String>
    private lateinit var exportFile: File

    private fun toast(msg: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(this, msg, duration).show()
    }

    private fun handleFileOperation(it: FileOperation) {
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
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        intentLauncher = registerForActivityResult(StartActivityForResult()) {
            viewModel.versions.value.forEach {
                it.value.refreshIfInstalledChanged()
            }
        }
        exportLauncher = registerForActivityResult(CreateDocument(Const.apkMineType)) {
            val uri = it ?: return@registerForActivityResult
            lifecycleScope.launch {
                contentResolver.openOutputStream(uri)?.use { o ->
                    exportFile.inputStream().use { i -> i.copyTo(o) }
                }
            }
        }
        lifecycleScope.launch {
            val loadedVersions: Map<String, VersionViewModel>
            if (viewModel.loaded.value) {
                loadedVersions = viewModel.versions.value
            } else {
                loadedVersions = sortedMapOf<String, VersionViewModel>()
                JenkinsApi.getAllAndroidJobs().forEach { (job, buildNumbers) ->
                    val model = JenkinsVersionViewModel(job, buildNumbers)
                    loadedVersions[model.name] = model
                }
                FDroidApi.getAllPackages().forEach {
                    val model = FDroidVersionViewModel(it)
                    loadedVersions[model.name] = model
                }
                viewModel.versions.value = loadedVersions
                viewModel.loaded.value = true
            }
            loadedVersions.forEach { (jobName, vvm) ->
                vvm.toastMessage.onEach { toast("$jobName: $it") }.launchIn(this)
                vvm.fileOperation.onEach { handleFileOperation(it) }.launchIn(this)
            }
        }
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(0)
        )
        setContent {
            Fcitx5ForAndroidUpdaterTheme {
                val loaded by viewModel.loaded.collectAsState()
                val versions by viewModel.versions.collectAsState()
                MainScreen(versions) { pv, nc, v ->
                    if (loaded) NavScreen(paddingValues = pv, navController = nc, viewModels = v)
                    else LoadingScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MainScreen(
    viewModels: Map<String, VersionViewModel>,
    content: @Composable (PaddingValues, NavHostController, viewModels: Map<String, VersionViewModel>) -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val scaffoldState = rememberScaffoldState()
    val scope = rememberCoroutineScope()
    val scrimColor = Color.Black.copy(DrawerDefaults.ScrimOpacity)
    Scaffold(
        scaffoldState = scaffoldState,
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal)),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                backgroundColor = MaterialTheme.colors.primarySurface,
                windowInsets = AppBarDefaults.topAppBarWindowInsets,
                navigationIcon = {
                    IconButton(onClick = { scope.launch { scaffoldState.drawerState.open() } }) {
                        Icon(imageVector = Icons.Default.Menu, contentDescription = null)
                    }
                }
            )
        },
        drawerScrimColor = scrimColor,
        drawerContent = {
            Box {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .windowInsetsTopHeight(WindowInsets.statusBars)
                        .background(scrimColor)
                )
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                    viewModels.forEach { (name, _) ->
                        val selected = navBackStackEntry?.destination?.route == name
                        val color =
                            if (selected) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface
                        val icon = when (name) {
                            "fcitx5-android" -> Icons.Default.Keyboard
                            "fcitx5-android-updater" -> Icons.Default.SystemUpdate
                            else -> Icons.Default.Extension
                        }
                        ListItem(
                            modifier = Modifier.clickable {
                                scope.launch {
                                    scaffoldState.drawerState.close()
                                }
                                navController.navigate(name) {
                                    // clear navigation stack before navigation
                                    popUpTo(0)
                                }
                            },
                            icon = { Icon(icon, contentDescription = null, tint = color) },
                            text = {
                                Text(
                                    text = name.removePrefix("fcitx5-android-"),
                                    color = color,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        )
                    }
                    Spacer(
                        Modifier
                            .padding(bottom = 16.dp)
                            .windowInsetsBottomHeight(WindowInsets.navigationBars)
                    )
                }
            }
        }
    ) { paddingValues ->
        content(paddingValues, navController, viewModels)
    }
    BackHandler(enabled = scaffoldState.drawerState.isOpen) {
        scope.launch {
            scaffoldState.drawerState.close()
        }
    }
}

@Composable
fun LoadingScreen() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp),
            strokeWidth = 5.dp,
            strokeCap = StrokeCap.Square
        )
    }
}

@Composable
fun NavScreen(
    paddingValues: PaddingValues,
    navController: NavHostController,
    viewModels: Map<String, VersionViewModel>
) {
    NavHost(
        navController = navController,
        startDestination = viewModels.keys.first(),
        modifier = Modifier.padding(paddingValues),
        contentAlignment = Alignment.TopCenter
    ) {
        viewModels.forEach { (jobName, viewModel) ->
            composable(jobName) {
                VersionScreen(viewModel)
            }
        }
    }
}

val LocalVersionViewModel = compositionLocalOf<VersionViewModel> { error("No view model") }

@Composable
fun versionViewModel() = LocalVersionViewModel.current

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun VersionScreen(viewModel: VersionViewModel) {
    CompositionLocalProvider(LocalVersionViewModel provides viewModel) {
        LaunchedEffect(viewModel) {
            if (!viewModel.hasRefreshed) {
                viewModel.refresh()
            }
        }
        val refreshing by viewModel.isRefreshing.collectAsState()
        val pullRefreshState = rememberPullRefreshState(refreshing, { viewModel.refresh() })
        val urlHandler = LocalUriHandler.current
        Box(Modifier.fillMaxSize().pullRefresh(pullRefreshState)) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            urlHandler.openUri(viewModel.url)
                        },
                    elevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.github_mark),
                            contentDescription = "GitHub Logo",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colors.onSurface
                        )
                        Text(
                            text = viewModel.name,
                            modifier = Modifier.padding(start = 10.dp),
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.body1
                        )
                    }
                }
                Versions(
                    stringResource(R.string.installed),
                    listOf(viewModel.installedVersion)
                )
                Versions(
                    stringResource(R.string.versions),
                    viewModel.sortedVersions
                )
                Spacer(
                    Modifier
                        .padding(bottom = 16.dp)
                        .windowInsetsBottomHeight(WindowInsets.navigationBars)
                )
            }
            PullRefreshIndicator(refreshing, pullRefreshState, Modifier.align(Alignment.TopCenter))
        }
    }
}
