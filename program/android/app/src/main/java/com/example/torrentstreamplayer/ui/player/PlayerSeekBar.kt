package com.example.torrentstreamplayer.ui.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.Preview
import com.example.torrentstreamplayer.engine.PieceCell
import com.example.torrentstreamplayer.engine.PieceMapSnapshot
import com.example.torrentstreamplayer.ui.theme.BrandColors
import com.example.torrentstreamplayer.ui.theme.PlaybackColors
import com.example.torrentstreamplayer.ui.theme.Shapes
import com.example.torrentstreamplayer.ui.theme.Sizes
import com.example.torrentstreamplayer.ui.theme.TorrentStreamPlayerTheme

/**
 * 재생 막대: 트랙(`outline`) 위에 받은 구간(`PlaybackColors.Downloaded`)을 조각 맵 순서대로 칠하고,
 * 재생한 구간은 브랜드 그라데이션으로 덮는다. 손잡이는 영상 위 흰색.
 * 조각 맵은 파일 앞 → 뒤 순서라 영상 시간과 대략 겹친다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlayerSeekBar(
    position: Float,
    pieces: PieceMapSnapshot,
    enabled: Boolean,
    onChange: (Float) -> Unit,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    Slider(
        value = position.coerceIn(0f, 1f),
        onValueChange = onChange,
        onValueChangeFinished = onFinished,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        thumb = {
            Box(Modifier.size(Sizes.seekThumb).background(PlaybackColors.OnVideo, CircleShape))
        },
        track = { SeekTrack(position, pieces) }
    )
}

@Composable
private fun SeekTrack(position: Float, pieces: PieceMapSnapshot) {
    val played = position.coerceIn(0f, 1f)
    Canvas(Modifier.fillMaxWidth().height(Sizes.seekTrackHeight).clip(Shapes.pill)) {
        drawRect(PlaybackColors.Track)
        if (pieces.cells.isNotEmpty()) {
            val step = size.width / pieces.cells.size
            pieces.cells.forEachIndexed { index, cell ->
                if (cell != PieceCell.MISSING) {
                    drawRect(PlaybackColors.Downloaded, Offset(index * step, 0f), Size(step, size.height))
                }
            }
        }
        if (played > 0f) {
            val brush = Brush.horizontalGradient(listOf(BrandColors.Primary, BrandColors.Accent), 0f, size.width)
            drawRect(brush, size = Size(size.width * played, size.height))
        }
    }
}

@Preview
@Composable
private fun PlayerSeekBarPreview() {
    TorrentStreamPlayerTheme {
        Box(Modifier.background(PlaybackColors.Video)) {
            PlayerSeekBar(0.3f, PieceMapSnapshot.capture(0, 99, 4) { it < 60 }, true, {}, {})
        }
    }
}
