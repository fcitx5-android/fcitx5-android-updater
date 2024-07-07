package org.fcitx.fcitx5.android.updater.model

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.updater.PackageUtils
import org.fcitx.fcitx5.android.updater.UpdaterApplication
import org.fcitx.fcitx5.android.updater.bytesToMiB
import org.fcitx.fcitx5.android.updater.externalDir
import org.fcitx.fcitx5.android.updater.network.DownloadEvent
import org.fcitx.fcitx5.android.updater.network.DownloadTask
import java.io.File

abstract class VersionViewModel(
    val name: String,
    val pkgName: String,
    val url: String
) : ViewModel() {

    var hasRefreshed = false
        protected set

    protected val _isRefreshing = MutableStateFlow(false)

    private val remoteVersionUiStates: MutableMap<VersionUi.Remote, MutableStateFlow<RemoteVersionUiState>> =
        mutableMapOf()

    private val remoteDownloadTasks: MutableMap<VersionUi.Remote, DownloadTask?> =
        mutableMapOf()

    val isRefreshing: StateFlow<Boolean>
        get() = _isRefreshing.asStateFlow()

    var installedVersion by mutableStateOf<VersionUi>(VersionUi.NotInstalled)
        private set
    protected val remoteVersions = mutableMapOf<String, VersionUi.Remote>()
    protected val localVersions = mutableMapOf<String, VersionUi.Local>()

    protected val allVersions = mutableStateMapOf<String, VersionUi>()

    private val VersionUi.isNowInstalled
        get() = installedVersion.versionName == versionName

    private var lastVersionName = ""

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage = _toastMessage.asSharedFlow()

    private val _fileOperation = MutableSharedFlow<FileOperation>()
    val fileOperation = _fileOperation.asSharedFlow()

    private val downloadDir = File(externalDir, name).apply {
        mkdirs()
    }

    fun getRemoteUiState(remote: VersionUi.Remote) =
        remoteVersionUiStates.getOrPut(remote) {
            MutableStateFlow(RemoteVersionUiState.Idle(true))
        }.asStateFlow()

    fun uninstall() {
        viewModelScope.launch {
            _fileOperation.emit(FileOperation.Uninstall(pkgName))
        }
    }

    fun download(remote: VersionUi.Remote) {
        val flow = remoteVersionUiStates.getValue(remote)
        val task = DownloadTask(remote.downloadUrl, File(downloadDir, remote.versionName + ".apk"))
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
                        remote.versionCode,
                        pkgName,
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

    fun getRemoteUrl(local: VersionUi.Local): String? {
        return remoteVersions[local.versionName]?.downloadUrl
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

    fun install(local: VersionUi.Local) {
        viewModelScope.launch {
            _fileOperation.emit(FileOperation.Install(local.archiveFile))
        }
    }

    fun share(local: VersionUi.Local) {
        viewModelScope.launch {
            _fileOperation.emit(FileOperation.Share(local.archiveFile, local.displayName))
        }
    }

    fun export(local: VersionUi.Local) {
        viewModelScope.launch {
            _fileOperation.emit(FileOperation.Export(local.archiveFile, local.displayName))
        }
    }

    fun exportInstalled() {
        val installedPath =
            PackageUtils.getInstalledPath(UpdaterApplication.context, pkgName) ?: return
        viewModelScope.launch {
            _fileOperation.emit(
                FileOperation.Export(File(installedPath), installedVersion.displayName)
            )
        }
    }

    init {
        refreshInstalledVersion()
        refreshLocalVersions()
    }

    private fun getInstalled(context: Context) =
        PackageUtils.getInstalledVersionInfo(context, pkgName)
            ?.let { (versionName, versionCode) ->
                PackageUtils.getInstalledSize(context, pkgName)
                    ?.let { size ->
                        VersionUi.Installed(versionCode, pkgName, versionName, size)
                    }
            } ?: VersionUi.NotInstalled

    fun refreshIfInstalledChanged() {
        if (getInstalled(UpdaterApplication.context).versionName != lastVersionName)
            refresh()
    }

    fun refreshInstalledVersion() {
        installedVersion = getInstalled(UpdaterApplication.context)
        lastVersionName = installedVersion.versionName
    }

    fun refreshLocalVersions() {
        localVersions.clear()
        downloadDir
            .listFiles { file: File -> file.extension == "apk" }
            ?.mapNotNull {
                PackageUtils.getVersionInfo(UpdaterApplication.context, it.absolutePath)
                    ?.let { (versionName, versionCode) ->
                        VersionUi.Local(
                            versionCode,
                            pkgName,
                            versionName,
                            // Bytes to MiB
                            bytesToMiB(it.length()),
                            installedVersion.versionName == versionName,
                            it
                        )
                    }
            }
            ?.forEach {
                localVersions[it.versionName] = it
                allVersions[it.versionName] = it
            }
    }

    abstract fun refresh()

    abstract val sortedVersions: List<VersionUi>
}
