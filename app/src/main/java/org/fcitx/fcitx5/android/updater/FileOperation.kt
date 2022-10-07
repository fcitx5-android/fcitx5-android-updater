package org.fcitx.fcitx5.android.updater

import java.io.File

sealed interface FileOperation {
    object Uninstall : FileOperation
    class Install(val file: File) : FileOperation
    class Share(val file: File, val name: String) : FileOperation
    class Export(val file: File, val name: String) : FileOperation
}
