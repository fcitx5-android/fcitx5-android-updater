package org.fcitx.fcitx5.android.updater.utils

fun parseVersionNumber(raw: String): Result<Triple<String, Int, String>> = runCatching {
    val g = raw.split('-')
    require(g.size == 3)
    val tag = g[0]
    val commitInc = g[1].toInt()
    val hash = g[2].drop(1)
    Triple(tag, commitInc, hash)
}
