package org.fcitx.fcitx5.android.updater

import java.io.File

sealed interface FileOperation {
    data class Uninstall(val packageName: String) : FileOperation
    data class Install(val file: File) : FileOperation
    data class Share(val file: File, val name: String) : FileOperation
    data class Export(val file: File, val name: String) : FileOperation
}
