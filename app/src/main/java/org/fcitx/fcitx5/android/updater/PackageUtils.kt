package org.fcitx.fcitx5.android.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object PackageUtils {

    fun getVersionName(context: Context, apkFilePath: String) =
        context.packageManager.getPackageArchiveInfo(apkFilePath, 0)?.versionName

    fun getInstalledVersionName(context: Context, packageName: String) =
        runCatching {
            context.packageManager.getPackageInfo(
                packageName,
                0
            ).versionName
        }.getOrNull()


    fun getInstalledSize(context: Context, packageName: String) = runCatching {
        context.packageManager.getApplicationInfo(
            packageName,
            0
        ).publicSourceDir.let { src ->
            File(src)
                .length()
                .takeIf { it != 0L }
                // Bytes to MB
                ?.let { it / 1E6 }
        }
    }.getOrNull()

    fun installIntent(apkFilePath: String) =
        Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(
                FileProvider.getUriForFile(
                    MyApplication.context,
                    BuildConfig.APPLICATION_ID + ".provider",
                    File(apkFilePath)
                ), "application/vnd.android.package-archive"
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

    fun uninstallIntent(packageName: String) =
        Intent(Intent.ACTION_DELETE).apply {
            data = Uri.parse("package:$packageName")
        }
}