package org.fcitx.fcitx5.android.updater

import android.content.Context
import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
        private set
    private var remoteVersions = mutableMapOf<String, VersionUi.Remote>()
    private val localVersions = mutableMapOf<String, VersionUi.Local>()

    val allVersions = mutableStateMapOf<String, VersionUi>()

    private val VersionUi.isNowInstalled
        get() = installedVersion.versionName == versionName

    private var lastVersionName = ""

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage = _toastMessage.asSharedFlow()

    fun getRemoteUiState(remote: VersionUi.Remote) =
        remoteVersionUiStates.getOrPut(remote) {
            MutableStateFlow(RemoteVersionUiState.Idle(true))
        }.asStateFlow()

    fun uninstall(
        launcher: ManagedActivityResultLauncher<Intent, ActivityResult>,
    ) {
        launcher.launch(PackageUtils.uninstallIntent(Const.fcitx5AndroidPackageName))
    }

    fun download(remote: VersionUi.Remote) {
        val flow = remoteVersionUiStates.getValue(remote)
        val task = DownloadTask(remote.downloadUrl, File(externalDir, remote.versionName + ".apk"))
        var progress = .0f
        task.eventFlow.onEach { event ->
            when (event) {
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
                    val local = VersionUi.Local(
                        remote.versionName,
                        remote.size,
                        remote.isNowInstalled,
                        task.file
                    )
                    localVersions[remote.versionName] = local
                    allVersions[remote.versionName] = local
                }
                is DownloadEvent.Failed -> {
                    _toastMessage.emit(event.cause.message ?: event.cause.stackTraceToString())
                }
                DownloadEvent.Paused -> {
                    flow.emit(RemoteVersionUiState.Pausing(true, progress))
                }
                DownloadEvent.Purged -> {
                    flow.emit(RemoteVersionUiState.Idle(true))
                }
                is DownloadEvent.Downloading -> {
                    progress = event.progress.toFloat()
                    flow.emit(RemoteVersionUiState.Downloading(true, progress))
                }
                DownloadEvent.StartPurging -> {
                    flow.emit(RemoteVersionUiState.Downloading(false, progress))
                }
                DownloadEvent.StartWaitingRetry -> {
                    flow.emit(RemoteVersionUiState.WaitingRetry)
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
        val version = local.versionName
        localVersions.remove(version)
        remoteVersions[version]?.let {
            allVersions[version] = it
            viewModelScope.launch {
                remoteVersionUiStates[it]?.emit(RemoteVersionUiState.Idle(true))
            }
        } ?: run {
            allVersions.remove(version)
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
                ?.forEach {
                    localVersions[it.versionName] = it
                    allVersions[it.versionName] = it
                }
            remoteVersions.clear()
            JenkinsApi.getAllWorkflowRuns(Const.fcitx5AndroidJenkinsJobName)
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
                        artifact.url
                    )
                }
                .forEach {
                    remoteVersions[it.versionName] = it
                    if (it.versionName !in localVersions)
                        allVersions[it.versionName] = it
                }
            _isRefreshing.emit(false)
        }
    }

}