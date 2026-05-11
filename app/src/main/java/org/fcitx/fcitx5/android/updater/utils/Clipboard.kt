package org.fcitx.fcitx5.android.updater.utils

import android.content.ClipData
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import kotlinx.coroutines.runBlocking

fun Clipboard.copyText(text: String) {
    runBlocking {
        setClipEntry(ClipEntry(ClipData.newPlainText(text, text)))
    }
}
