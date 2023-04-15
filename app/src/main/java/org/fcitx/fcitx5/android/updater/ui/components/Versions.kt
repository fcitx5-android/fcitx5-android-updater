package org.fcitx.fcitx5.android.updater.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.fcitx.fcitx5.android.updater.VersionUi

@Composable
fun Versions(name: String, versions: List<VersionUi>) {
    Column {
        Text(
            text = name,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.h6
        )
        Surface(elevation = 2.dp) {
            Column {
                val last = versions.size - 1
                val dividerColor = MaterialTheme.colors.onSurface.copy(alpha = 0.06f)
                versions.forEachIndexed { index, version ->
                    VersionCard(version)
                    if (index != last) Divider(color = dividerColor)
                }
            }
        }
    }
}
