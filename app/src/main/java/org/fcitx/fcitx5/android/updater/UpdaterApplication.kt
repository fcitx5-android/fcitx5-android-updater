package org.fcitx.fcitx5.android.updater

import android.app.Application
import android.content.Context

class UpdaterApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        private var instance: UpdaterApplication? = null

        val context: Context
            get() = instance!!.applicationContext
    }
}