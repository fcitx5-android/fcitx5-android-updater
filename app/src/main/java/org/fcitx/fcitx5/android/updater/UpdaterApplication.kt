package org.fcitx.fcitx5.android.updater

import android.app.Application
import android.content.Context

class UpdaterApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        migrateOldDownloads()
    }

    private fun migrateOldDownloads() {
        val apks = externalDir.listFiles { _, name -> name.endsWith(".apk") } ?: return
        val appDownloadDir = externalDir.resolve("fcitx5-android")
        apks.forEach {
            it.renameTo(appDownloadDir.resolve(it.name))
        }
    }

    companion object {
        private var instance: UpdaterApplication? = null

        val context: Context
            get() = instance!!.applicationContext
    }
}