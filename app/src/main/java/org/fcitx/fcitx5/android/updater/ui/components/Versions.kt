package org.fcitx.fcitx5.android.updater.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.fcitx.fcitx5.android.updater.model.VersionUi

@Composable
fun Versions(name: String, versions: List<VersionUi>) {
    Column {
        Text(
            text = name,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.titleLarge
        )
        Surface( shadowElevation = 2.dp) {
            Column {
                val last = versions.size - 1
                val dividerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                versions.forEachIndexed { index, version ->
                    VersionCard(version)
                    if (index != last) Divider(color = dividerColor)
                }
            }
        }
    }
}
