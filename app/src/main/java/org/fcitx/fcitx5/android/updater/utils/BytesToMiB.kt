package org.fcitx.fcitx5.android.updater.utils

import kotlin.math.pow

fun bytesToMiB(src: Long) = src.toDouble() / 2.0.pow(20)
