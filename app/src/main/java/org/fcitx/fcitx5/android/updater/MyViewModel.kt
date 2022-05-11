package org.fcitx.fcitx5.android.updater

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.updater.api.GitHubApi
import org.fcitx.fcitx5.android.updater.api.JenkinsApi

class MyViewModel : ViewModel() {
    private val _isRefreshing = MutableStateFlow(false)

    val isRefreshing: StateFlow<Boolean>
        get() = _isRefreshing.asStateFlow()

    var installedVersion by mutableStateOf(listOf<VersionUi.Installed>())

    var remoteVersion by mutableStateOf(listOf<VersionUi.Remote>())

    val localVersion = mutableStateListOf<VersionUi.Local>()

    fun uninstall(installed: VersionUi.Installed) {
        Log.d("m", "uninstall")
    }

    fun download(remote: VersionUi.Remote) {
        Log.d("m", "download")
    }

    fun delete(local: VersionUi.Local) {
        Log.d("m", "delete")
    }

    fun install(local: VersionUi.Local) {
        Log.d("m", "install")
    }

    init {
        refresh()
    }

    private fun getInstalled(context: Context) =
        PackageUtils.getInstalledVersionName(context, Consts.fcitx5AndroidPackageName)
            ?.let { version ->
                PackageUtils.getInstalledSize(context, Consts.fcitx5AndroidPackageName)
                    ?.let { size ->
                        VersionUi.Installed(version, size)
                    }

            } ?: VersionUi.NotInstalled

    fun refresh() {
        if (isRefreshing.value)
            return
        viewModelScope.launch {
            _isRefreshing.emit(true)
            installedVersion = listOf(getInstalled(MyApplication.context))
            remoteVersion = JenkinsApi.getAllWorkflowRuns(Consts.fcitx5AndroidJenkinsJobName)
                .mapNotNull { it.getOrNull() }
                .parallelMap {
                    it to JenkinsApi.getArtifactSize(it.artifacts.selectByABI()!!) to GitHubApi.getCommitNumber(
                        Consts.fcitx5AndroidGitHubOwner,
                        Consts.fcitx5AndroidGitHubRepo,
                        it.revision
                    )
                }
                .map { (p,commit) ->
                    VersionUi.Remote(
                        "${p.first.revision} (#${commit.getOrNull()})",
                        p.second ?: .0,
                        false,
                        false,
                        ""
                    )
                }
            _isRefreshing.emit(false)
        }
    }
}