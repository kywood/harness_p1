package com.example.torrentstreamplayer.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.torrentstreamplayer.R
import com.example.torrentstreamplayer.ui.components.AppBottomSheet

/** 더보기 하단 시트: 화면 확대(시트를 닫지 않고 여러 번 누른다), 링크 추가, 처음부터 재생. */
@Composable
internal fun PlayerMoreSheet(
    zoomLabel: String,
    onZoomOut: () -> Unit,
    onZoomReset: () -> Unit,
    onZoomIn: () -> Unit,
    onDismiss: () -> Unit,
    onAddLink: (() -> Unit)?,
    onRestart: () -> Unit,
) {
    AppBottomSheet(onDismiss = onDismiss) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.player_zoom), style = MaterialTheme.typography.bodyLarge) },
            leadingContent = {
                Icon(ImageVector.vectorResource(R.drawable.ic_zoom_in), contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary)
            },
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SheetIconButton(ImageVector.vectorResource(R.drawable.ic_zoom_out),
                        stringResource(R.string.cd_zoom_out), onZoomOut)
                    val resetDescription = zoomResetDescription(zoomLabel)
                    IconButton(onClick = onZoomReset,
                        modifier = Modifier.semantics { contentDescription = resetDescription }) {
                        Text(zoomLabel, color = MaterialTheme.colorScheme.onSurface, maxLines = 1,
                            style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = TABULAR_FIGURES))
                    }
                    SheetIconButton(ImageVector.vectorResource(R.drawable.ic_zoom_in),
                        stringResource(R.string.cd_zoom_in), onZoomIn)
                }
            },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        )
        if (onAddLink != null) SheetItem(Icons.Filled.Add, stringResource(R.string.action_add_link), onAddLink)
        SheetItem(Icons.Filled.Refresh, stringResource(R.string.player_restart), onRestart)
    }
}

/** "확대 배율 되돌리기 (1.5×)". */
@Composable
internal fun zoomResetDescription(label: String): String =
    stringResource(R.string.track_with_detail, stringResource(R.string.cd_zoom_reset), label)

@Composable
private fun SheetIconButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(icon, contentDescription = description, tint = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun SheetItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label, style = MaterialTheme.typography.bodyLarge) },
        leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    )
}
