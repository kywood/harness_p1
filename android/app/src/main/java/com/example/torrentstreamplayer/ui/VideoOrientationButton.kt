package com.example.torrentstreamplayer.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
internal fun VideoOrientationButton() {
    val activity = VideoOrientation.activity(LocalContext.current)
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    IconButton(enabled = activity != null,
        onClick = { activity?.requestedOrientation = VideoOrientation.next(landscape) }) {
        Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Refresh, contentDescription = VideoOrientation.description(landscape), tint = Color.White)
            Canvas(Modifier.size(if (landscape) 6.dp else 10.dp, if (landscape) 10.dp else 6.dp)) {
                drawRoundRect(Color.White, cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
            }
        }
    }
}
