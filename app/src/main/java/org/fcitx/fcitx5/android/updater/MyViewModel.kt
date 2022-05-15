package org.fcitx.fcitx5.android.updater

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.updater.api.CommonApi
import org.fcitx.fcitx5.android.updater.api.JenkinsApi
import org.fcitx.fcitx5.android.updater.network.DownloadEvent
import org.fcitx.fcitx5.android.updater.network.DownloadTask
import java.io.File

class MyViewModel : ViewModel() {
    private val _isRefreshing = MutableStateFlow(false)

    private val remoteUiStates: MutableMap<VersionUi.Remote, MutableStateFlow<RemoteUiState>> =
        mutableMapOf()

    private val remoteDownloadTasks: MutableMap<VersionUi.Remote, DownloadTask?> =
        mutableMapOf()

    val isRefreshing: StateFlow<Boolean>
        get() = _isRefreshing.asStateFlow()

    var installedVersion by mutableStateOf(VersionUi.NotInstalled)

    var remoteVersion by mutableStateOf(listOf<VersionUi.Remote>())

    val localVersion: MutableList<VersionUi.Local>

    fun getRemoteUiState(remote: VersionUi.Remote) =
        remoteUiStates.getValue(remote).asStateFlow()

    fun uninstall(
        launcher: ManagedActivityResultLauncher<Intent, ActivityResult>,
    ) {
        launcher.launch(PackageUtils.uninstallIntent(Const.fcitx5AndroidPackageName))
    }

    fun download(remote: VersionUi.Remote) {
        Log.d("m", "download")
        val flow = remoteUiStates.getValue(remote)
        val task = DownloadTask(remote.downloadUrl, File(externalDir, remote.versionName + ".tmp"))
        var progress = .0f
        task.eventFlow.onEach {
            Log.d("G", it.toString())
            when (it) {
                DownloadEvent.Created -> {
                    flow.emit(RemoteUiState.Pending)
                }
                DownloadEvent.Downloaded -> {
                    flow.emit(RemoteUiState.Downloaded)
                }
                is DownloadEvent.Failed -> {
                    flow.emit(RemoteUiState.Idle)
                }
                DownloadEvent.Paused -> {
                    flow.emit(RemoteUiState.Pausing(progress))
                }
                is DownloadEvent.Progressed -> {
                    progress = it.progress.toFloat()
                    flow.emit(RemoteUiState.Downloading(progress))
                }
                DownloadEvent.Purged -> {
                    flow.emit(RemoteUiState.Idle)
                }
                DownloadEvent.Resumed -> {
                    flow.emit(RemoteUiState.Pending)
                }
            }
        }.let {
            viewModelScope.launch(Dispatchers.Default) {
                it.collect()
            }
        }
        task.start()
        remoteDownloadTasks[remote] = task
    }

    fun pauseDownload(remote: VersionUi.Remote) {
        Log.d("m", "pause")
        remoteDownloadTasks[remote]?.pause()
    }


    fun resumeDownload(remote: VersionUi.Remote) {
        Log.d("m", "resume")
        remoteDownloadTasks[remote]?.resume()
    }

    fun cancelDownload(remote: VersionUi.Remote) {
        Log.d("m", "cancel")
        remoteDownloadTasks[remote]?.purge()
    }

    fun delete(local: VersionUi.Local) {
        Log.d("m", "delete")
    }

    fun install(local: VersionUi.Local) {
        Log.d("m", "install")
    }

    init {
        localVersion = mutableStateListOf()
        refresh()
    }

    private fun getInstalled(context: Context) =
        PackageUtils.getInstalledVersionName(context, Const.fcitx5AndroidPackageName)
            ?.let { version ->
                PackageUtils.getInstalledSize(context, Const.fcitx5AndroidPackageName)
                    ?.let { size ->
                        VersionUi.Installed(version, size)
                    }

            } ?: VersionUi.NotInstalled

    fun onUninstall() {
        if (getInstalled(MyApplication.context) == VersionUi.NotInstalled)
            refresh()
    }

    fun refresh() {
        if (isRefreshing.value)
            return
        viewModelScope.launch {
            _isRefreshing.emit(true)
            remoteUiStates.clear()
            installedVersion = getInstalled(MyApplication.context)
            remoteVersion = JenkinsApi.getAllWorkflowRuns(Const.fcitx5AndroidJenkinsJobName)
                .mapNotNull {
                    it.getOrNull()?.artifacts?.selectByABI()?.let { artifact ->
                        artifact.extractVersionName()?.let { versionName ->
                            artifact to versionName
                        }
                    }
                }
                .parallelMap { (artifact, versionName) ->
                    VersionUi.Remote(
                        versionName,
                        // Bytes to MB
                        CommonApi.getContentLength(artifact.url)?.let { it / 1E6 } ?: .0,
                        versionName == installedVersion.versionName,
                        false,
                        artifact.url
                    )
                }
            remoteUiStates.putAll(remoteVersion.associateWith {
                MutableStateFlow(
                    if (it.isDownloaded)
                        RemoteUiState.Downloaded
                    else
                        RemoteUiState.Idle
                )
            })
            _isRefreshing.emit(false)
        }
    }
}