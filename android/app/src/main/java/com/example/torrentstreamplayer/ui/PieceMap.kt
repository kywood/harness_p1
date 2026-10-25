package com.example.torrentstreamplayer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.torrentstreamplayer.engine.PieceCell
import com.example.torrentstreamplayer.engine.PieceMapSnapshot
import com.example.torrentstreamplayer.ui.theme.PlaybackColors

@Composable
fun PieceMap(snapshot: PieceMapSnapshot, modifier: Modifier = Modifier,
    labelColor: Color = MaterialTheme.colorScheme.onSurface, compact: Boolean = false, inlineInfo: Boolean = false) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (!compact && !inlineInfo) Text("다운로드 조각 · 파일 용량 기준", color = labelColor, style = MaterialTheme.typography.labelSmall)
        Canvas(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(3.dp))) {
            drawRect(PlaybackColors.Track)
            if (snapshot.cells.isNotEmpty()) {
                val step = size.width / snapshot.cells.size
                val gap = minOf(1.dp.toPx(), step * 0.2f)
                snapshot.cells.forEachIndexed { index, cell ->
                    val color = when (cell) {
                        PieceCell.VERIFIED -> PlaybackColors.Accent
                        PieceCell.PARTIAL -> PlaybackColors.PiecePartial
                        PieceCell.LOW -> PlaybackColors.PiecePartial.copy(alpha = 0.35f)
                        PieceCell.MEDIUM -> PlaybackColors.PiecePartial.copy(alpha = 0.65f)
                        PieceCell.HIGH -> PlaybackColors.PiecePartial
                        PieceCell.MISSING -> PlaybackColors.Track
                    }
                    drawRect(color, Offset(index * step, 0f), Size(step - gap, size.height))
                }
            }
        }
        if (!compact) Row(Modifier.fillMaxWidth().then(if (inlineInfo) Modifier.horizontalScroll(rememberScrollState()) else Modifier),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (inlineInfo) Text(if (snapshot.total == 0) "피스 정보 확인 전" else "피스 ${snapshot.verified}/${snapshot.total} · 파일 용량 기준",
                color = labelColor, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            PieceLegend("완료", PlaybackColors.Accent, labelColor)
            PieceLegend("1–33%", PlaybackColors.PiecePartial.copy(alpha = 0.35f), labelColor)
            PieceLegend("34–66%", PlaybackColors.PiecePartial.copy(alpha = 0.65f), labelColor)
            PieceLegend("67–99%", PlaybackColors.PiecePartial, labelColor)
            PieceLegend("미완료", PlaybackColors.Track, labelColor)
        }
        if (!inlineInfo) Text(if (snapshot.total == 0) "피스 정보 확인 전" else
            "파일 앞 → 뒤 · 피스 " + snapshot.verified + " / " + snapshot.total +
                if (snapshot.total > snapshot.cells.size) " · 한 칸에 여러 피스" else "",
            color = labelColor, style = MaterialTheme.typography.labelSmall)
    }
}
@Composable
private fun PieceLegend(label: String, color: Color, labelColor: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(10.dp).background(color, RoundedCornerShape(2.dp)))
        Text(label, color = labelColor, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}
