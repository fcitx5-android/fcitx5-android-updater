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

    private val remoteVersionUiStates: MutableMap<VersionUi.Remote, MutableStateFlow<RemoteVersionUiState>> =
        mutableMapOf()

    private val remoteDownloadTasks: MutableMap<VersionUi.Remote, DownloadTask?> =
        mutableMapOf()

    val isRefreshing: StateFlow<Boolean>
        get() = _isRefreshing.asStateFlow()

    var installedVersion by mutableStateOf(VersionUi.NotInstalled)

    var remoteVersions = mutableStateListOf<VersionUi.Remote>()
    val localVersions = mutableStateListOf<VersionUi.Local>()

    private val VersionUi.isNowInstalled
        get() = installedVersion.versionName == versionName

    private var lastVersionName = ""

    fun getRemoteUiState(remote: VersionUi.Remote) =
        remoteVersionUiStates.getOrPut(remote) {
            MutableStateFlow(
                if (remote.isDownloaded)
                    RemoteVersionUiState.Downloaded
                else
                    RemoteVersionUiState.Idle(true)
            )
        }.asStateFlow()

    fun uninstall(
        launcher: ManagedActivityResultLauncher<Intent, ActivityResult>,
    ) {
        launcher.launch(PackageUtils.uninstallIntent(Const.fcitx5AndroidPackageName))
    }

    fun download(remote: VersionUi.Remote) {
        Log.d("m", "download")
        val flow = remoteVersionUiStates.getValue(remote)
        val task = DownloadTask(remote.downloadUrl, File(externalDir, remote.versionName + ".apk"))
        var progress = .0f
        task.eventFlow.onEach {
            Log.e("g", it.toString())
            when (it) {
                DownloadEvent.StartCreating -> {
                    flow.emit(RemoteVersionUiState.Idle(false))
                }
                DownloadEvent.Created -> {
                    flow.emit(RemoteVersionUiState.Pending)
                }
                DownloadEvent.StartPausing -> {
                    flow.emit(RemoteVersionUiState.Downloading(false, progress))
                }
                DownloadEvent.StartResuming -> {
                    flow.emit(RemoteVersionUiState.Pausing(false, progress))
                }
                DownloadEvent.Resumed -> {
                    flow.emit(RemoteVersionUiState.Pending)
                }
                DownloadEvent.Downloaded -> {
                    flow.emit(RemoteVersionUiState.Downloaded)
                    localVersions.add(
                        VersionUi.Local(
                            remote.versionName,
                            remote.size,
                            remote.isNowInstalled,
                            task.file
                        )
                    )
                }
                is DownloadEvent.Failed -> {
                    flow.emit(RemoteVersionUiState.Idle(true))
                }
                DownloadEvent.Paused -> {
                    flow.emit(RemoteVersionUiState.Pausing(true, progress))
                }
                DownloadEvent.Purged -> {
                    flow.emit(RemoteVersionUiState.Idle(true))
                }
                is DownloadEvent.Downloading -> {
                    progress = it.progress.toFloat()
                    flow.emit(RemoteVersionUiState.Downloading(true, progress))
                }
                DownloadEvent.StartPurging -> {
                    flow.emit(RemoteVersionUiState.Downloading(false, progress))
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
        remoteDownloadTasks[remote]?.pause()
    }


    fun resumeDownload(remote: VersionUi.Remote) {
        remoteDownloadTasks[remote]?.resume()
    }

    fun cancelDownload(remote: VersionUi.Remote) {
        remoteDownloadTasks[remote]?.purge()
    }

    fun delete(local: VersionUi.Local) {
        local.archiveFile.delete()
        localVersions.remove(local)
        remoteVersions.find { it.versionName == local.versionName }?.let {
            viewModelScope.launch {
                remoteVersionUiStates.getValue(it).emit(RemoteVersionUiState.Idle(true))
            }
        }
    }

    fun install(
        launcher: ManagedActivityResultLauncher<Intent, ActivityResult>,
        local: VersionUi.Local
    ) {
        launcher.launch(PackageUtils.installIntent(local.archiveFile.path))
    }

    init {
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

    fun refreshIfInstalledChanged() {
        if (getInstalled(MyApplication.context).versionName != lastVersionName)
            refresh()
    }

    fun refresh() {
        if (isRefreshing.value)
            return
        viewModelScope.launch {
            _isRefreshing.emit(true)
            installedVersion = getInstalled(MyApplication.context)
            lastVersionName = installedVersion.versionName
            localVersions.clear()
            externalDir
                .listFiles { file: File -> file.extension == "apk" }
                ?.mapNotNull {
                    PackageUtils.getVersionName(MyApplication.context, it.absolutePath)
                        ?.let { versionName ->
                            VersionUi.Local(
                                versionName,
                                // Bytes to MB
                                it.length() / 1E6,
                                installedVersion.versionName == versionName,
                                it
                            )
                        }
                }
                ?.let { localVersions.addAll(it) }
            remoteVersions.clear()
            remoteVersions.addAll(JenkinsApi.getAllWorkflowRuns(Const.fcitx5AndroidJenkinsJobName)
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
                        CommonApi.getContentLength(artifact.url)
                            .getOrNull()
                            ?.let { it / 1E6 }
                            ?: .0,
                        versionName == installedVersion.versionName,
                        versionName in localVersions.map { it.versionName },
                        artifact.url
                    )
                }
            )
            _isRefreshing.emit(false)
        }
    }

}