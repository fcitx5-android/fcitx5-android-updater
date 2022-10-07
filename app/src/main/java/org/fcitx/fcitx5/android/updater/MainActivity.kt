package org.fcitx.fcitx5.android.updater

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.primarySurface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.accompanist.insets.navigationBarsHeight
import com.google.accompanist.insets.rememberInsetsPaddingValues
import com.google.accompanist.insets.ui.Scaffold
import com.google.accompanist.insets.ui.TopAppBar
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import net.swiftzer.semver.SemVer
import org.fcitx.fcitx5.android.updater.ui.components.VersionCard
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
                    val viewModel: MainViewModel = viewModel()
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
}

@Composable
fun Screen() {
    val viewModel: MainViewModel = viewModel()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(text = stringResource(R.string.app_name)) },
            backgroundColor = MaterialTheme.colors.primarySurface,
            contentPadding = rememberInsetsPaddingValues(
                LocalWindowInsets.current.statusBars,
                applyBottom = false,
            )
        )
    }) { paddingValues ->
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
                Spacer(Modifier.navigationBarsHeight(additional = 16.dp).fillMaxWidth())
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
