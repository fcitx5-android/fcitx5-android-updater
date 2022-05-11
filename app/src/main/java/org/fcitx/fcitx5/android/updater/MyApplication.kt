package org.fcitx.fcitx5.android.updater

import android.app.Application
import android.content.Context

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        private var instance: MyApplication? = null

        val context: Context
            get() = instance!!.applicationContext
    }
}