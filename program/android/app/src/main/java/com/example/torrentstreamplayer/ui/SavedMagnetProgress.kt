package com.example.torrentstreamplayer.ui

import android.text.format.Formatter
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.example.torrentstreamplayer.engine.SavedMagnet
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

@Composable
internal fun SavedMagnetProgress(entry: SavedMagnet, speedBytes: Long? = null) {
    val context = LocalContext.current
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val complete = entry.fileSize?.let { it > 0 && it == entry.downloadedBytes } == true
    val accent = if (complete) (if (dark) Color(0xFF6EE7B7) else Color(0xFF087F5B))
        else if (dark) Color(0xFF7CB7FF) else Color(0xFF1D5FC4)
    Text(DownloadMetricText.build(entry.fileSize, entry.downloadedBytes, speedBytes, accent,
        MaterialTheme.colorScheme.onSurfaceVariant, if (dark) Color(0xFFC4B5FD) else Color(0xFF7755B8)) {
            Formatter.formatShortFileSize(context, it) },
        style = MaterialTheme.typography.bodySmall, maxLines = 1,
        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
}
