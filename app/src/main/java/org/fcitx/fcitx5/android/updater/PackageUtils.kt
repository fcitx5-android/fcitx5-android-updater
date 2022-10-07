package org.fcitx.fcitx5.android.updater

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File
import kotlin.math.pow

object PackageUtils {

    fun getVersionName(context: Context, apkFilePath: String) =
        context.packageManager.run {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getPackageArchiveInfo(apkFilePath, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                getPackageArchiveInfo(apkFilePath, 0)
            }?.versionName
        }

    fun getInstalledVersionName(
        context: Context,
        packageName: String = Const.fcitx5AndroidPackageName
    ) = context.packageManager.runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            getPackageInfo(packageName, 0)
        }.versionName
    }.getOrNull()

    fun getInstalledPath(
        context: Context,
        packageName: String = Const.fcitx5AndroidPackageName
    ) = context.packageManager.runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getApplicationInfo(packageName, 0)
        }.publicSourceDir
    }.getOrNull()

    fun getInstalledSize(
        context: Context,
        packageName: String = Const.fcitx5AndroidPackageName
    ) = getInstalledPath(context, packageName)?.let { path ->
        File(path)
            .length()
            .takeIf { it != 0L }
            // Bytes to MiB
            ?.let { it / 2.0.pow(20) }
    }

    fun installIntent(apkFile: File) =
        Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(
                FileProvider.getUriForFile(
                    UpdaterApplication.context,
                    Const.updaterProviderName,
                    apkFile
                ),
                Const.apkMineType
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

    fun uninstallIntent(packageName: String = Const.fcitx5AndroidPackageName) =
        Intent(Intent.ACTION_DELETE).apply {
            data = Uri.parse("package:$packageName")
        }

    fun shareIntent(file: File, outputName: String) =
        Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TITLE, outputName)
            type = Const.apkMineType
            putExtra(
                Intent.EXTRA_STREAM,
                FileProvider.getUriForFile(
                    UpdaterApplication.context,
                    Const.updaterProviderName,
                    file,
                    outputName
                )
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

}