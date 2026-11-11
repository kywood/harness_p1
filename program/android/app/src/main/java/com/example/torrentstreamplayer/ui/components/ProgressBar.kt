package com.example.torrentstreamplayer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.example.torrentstreamplayer.ui.theme.BrandColors
import com.example.torrentstreamplayer.ui.theme.Shapes
import com.example.torrentstreamplayer.ui.theme.Sizes
import com.example.torrentstreamplayer.ui.theme.TorrentStreamPlayerTheme

/** 진행 막대: 받은 부분은 브랜드 그라데이션, 나머지는 `outline`. [progress]는 0..1. */
@Composable
fun ProgressBar(progress: Float, modifier: Modifier = Modifier) {
    val fraction = progress.coerceIn(0f, 1f)
    val track = MaterialTheme.colorScheme.outline
    Canvas(
        modifier
            .fillMaxWidth()
            .height(Sizes.progressBarHeight)
            .clip(Shapes.pill)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f) }
    ) {
        drawRect(track)
        if (fraction > 0f) {
            // 그라데이션은 막대 전체 폭 기준이라 진행할수록 민트가 드러난다.
            val brush = Brush.horizontalGradient(listOf(BrandColors.Primary, BrandColors.Accent), 0f, size.width)
            drawRect(brush, size = Size(size.width * fraction, size.height))
        }
    }
}

@Preview
@Composable
private fun ProgressBarPreview() {
    TorrentStreamPlayerTheme { ProgressBar(0.6f) }
}
