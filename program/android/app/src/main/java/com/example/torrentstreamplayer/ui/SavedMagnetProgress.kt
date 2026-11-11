package com.example.torrentstreamplayer.ui

import android.text.format.Formatter
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import com.example.torrentstreamplayer.engine.SavedMagnet

/** 받은 양 / 전체 크기 · 진행률 · 속도. 완료는 `accent`, 받는 중은 `primaryLight`로 수치를 강조한다. */
@Composable
internal fun SavedMagnetProgress(entry: SavedMagnet, modifier: Modifier = Modifier, speedBytes: Long? = null) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val accent = if (PlaylistOrdering.complete(entry)) colors.secondary else colors.tertiary
    Text(DownloadMetricText.build(entry.fileSize, entry.downloadedBytes, speedBytes, accent,
        colors.onSurfaceVariant, colors.onSurface) { Formatter.formatShortFileSize(context, it) },
        modifier, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
}
