package org.fcitx.fcitx5.android.updater.utils


import org.fcitx.fcitx5.android.updater.Const
import org.fcitx.fcitx5.android.updater.api.JenkinsArtifact

fun List<JenkinsArtifact>.selectByABI() =
    filter { it.fileName.endsWith(".apk") }.let { apks ->
        if (apks.size == 1)
            apks.first()
        else
            apks.find { it.fileName.contains(Const.deviceABI) }
    }

private val artifactNameRegex = "\\S*-([^-]+)-([^-]+)-g([^-]+)-\\S*".toRegex()

fun JenkinsArtifact.extractVersionName() = artifactNameRegex.find(fileName)?.let {
    val groups = it.groupValues
    groups.getOrNull(1)?.let { tag ->
        groups.getOrNull(2)?.toIntOrNull()?.let { commitInc ->
            groups.getOrNull(3)?.let { hash ->
                "$tag-$commitInc-g$hash"
            }
        }
    }
}
