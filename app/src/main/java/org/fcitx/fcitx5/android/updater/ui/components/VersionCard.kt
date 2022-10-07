package org.fcitx.fcitx5.android.updater.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import org.fcitx.fcitx5.android.updater.VersionUi

@Composable
fun VersionCard(version: VersionUi) {
    ConstraintLayout(Modifier.padding(16.dp).clickable {}) {
        val (title, size, menu, action) = createRefs()
        Text(text = version.versionName,
            style = MaterialTheme.typography.body1,
            modifier = Modifier.constrainAs(title) {
                top.linkTo(parent.top)
                start.linkTo(parent.start)
            })
        Text(text = String.format("%.2f MiB", version.size),
            style = MaterialTheme.typography.body2,
            modifier = Modifier.constrainAs(size) {
                top.linkTo(title.bottom, 4.dp)
                start.linkTo(parent.start)
            })
        Box(modifier = Modifier.constrainAs(menu) {
            top.linkTo(parent.top)
            end.linkTo(parent.end)
        }) {
            var menuExpanded by remember { mutableStateOf(false) }
            val dismissMenu = { menuExpanded = false }
            IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(48.dp)) {
                Icon(imageVector = Icons.Filled.MoreVert, contentDescription = null)
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = dismissMenu,
                modifier = Modifier.defaultMinSize(minWidth = 180.dp),
                offset = DpOffset((-8).dp, (-56).dp)
            ) {
                when (version) {
                    is VersionUi.Installed -> VersionCardMenuInstalled(version, dismissMenu)
                    is VersionUi.Local -> VersionCardMenuLocal(version, dismissMenu)
                    is VersionUi.Remote -> VersionCardMenuRemote(version, dismissMenu)
                }
            }
        }
        val modifier = Modifier
            .fillMaxWidth()
            .constrainAs(action) {
                top.linkTo(menu.bottom)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            }
        when (version) {
            is VersionUi.Installed -> VersionCardActionInstalled(version, modifier)
            is VersionUi.Local -> VersionCardActionLocal(version, modifier)
            is VersionUi.Remote -> VersionCardActionRemote(version, modifier)
        }
    }
}
