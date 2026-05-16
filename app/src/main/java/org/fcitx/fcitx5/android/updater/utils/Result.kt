package org.fcitx.fcitx5.android.updater.utils

inline fun <T, U> Result<T>.flatMap(block: (T) -> Result<U>) =
    if (isFailure)
        Result.failure(exceptionOrNull()!!)
    else
        block(getOrNull()!!)

@Suppress("NOTHING_TO_INLINE")
inline fun <T> Iterable<Result<T>>.catResults() = mapNotNull { it.getOrNull() }
