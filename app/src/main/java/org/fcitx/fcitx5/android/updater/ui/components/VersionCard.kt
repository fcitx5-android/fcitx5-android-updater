package org.fcitx.fcitx5.android.updater.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import org.fcitx.fcitx5.android.updater.model.VersionUi

@Composable
fun VersionCard(version: VersionUi) {
    Box(Modifier.clickable { }) {
        ConstraintLayout(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp)
        ) {
            val (title, size, menu, action) = createRefs()
            Text(
                text = version.versionName,
                style = MaterialTheme.typography.body1,
                modifier = Modifier.constrainAs(title) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                }
            )
            CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                Text(
                    text = String.format("%.2f MiB", version.size),
                    style = MaterialTheme.typography.body2,
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .constrainAs(size) {
                            top.linkTo(title.bottom, 4.dp)
                            start.linkTo(parent.start)
                        }
                )
            }
            VersionCardMenu(version, modifier = Modifier.constrainAs(menu) {
                top.linkTo(parent.top)
                end.linkTo(parent.end)
            })
            VersionCardAction(version, modifier = Modifier.constrainAs(action) {
                width = Dimension.fillToConstraints
                top.linkTo(menu.bottom)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            })
        }
    }
}
