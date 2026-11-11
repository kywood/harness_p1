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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.torrentstreamplayer.ui.theme.PlaybackColors
import com.example.torrentstreamplayer.ui.theme.Sizes
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/** 회전 아이콘 안 기기 모양의 짧은 변·긴 변·선 두께. 아이콘 그림의 일부라 간격 토큰이 아니다. */
private val DEVICE_SHORT = 8.dp
private val DEVICE_LONG = 12.dp
private val DEVICE_STROKE = 1.dp

@Composable
internal fun VideoOrientationButton() {
    val activity = VideoOrientation.activity(LocalContext.current)
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    IconButton(enabled = activity != null,
        onClick = { activity?.requestedOrientation = VideoOrientation.next(landscape) }) {
        Box(Modifier.size(Sizes.iconMedium), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Refresh, contentDescription = VideoOrientation.description(landscape),
                tint = PlaybackColors.OnVideo, modifier = Modifier.size(Sizes.iconMedium))
            // 화면 방향을 나타내는 작은 직사각형(가로면 세로로 긴 모양: 다음에 바뀔 방향).
            Canvas(Modifier.size(if (landscape) DEVICE_SHORT else DEVICE_LONG, if (landscape) DEVICE_LONG else DEVICE_SHORT)) {
                drawRoundRect(PlaybackColors.OnVideo, cornerRadius = CornerRadius(DEVICE_STROKE.toPx()),
                    style = Stroke(DEVICE_STROKE.toPx()))
            }
        }
    }
}
