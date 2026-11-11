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
import androidx.compose.ui.res.stringResource
import com.example.torrentstreamplayer.R
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.torrentstreamplayer.engine.PieceMapSnapshot
import com.example.torrentstreamplayer.ui.theme.PieceMapColors

@Composable
fun PieceMap(snapshot: PieceMapSnapshot, modifier: Modifier = Modifier,
    labelColor: Color = MaterialTheme.colorScheme.onSurface, compact: Boolean = false, inlineInfo: Boolean = false,
    showInfo: Boolean = true) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (!compact && !inlineInfo) Text(stringResource(R.string.piece_map_title), color = labelColor, style = MaterialTheme.typography.labelSmall)
        Canvas(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(3.dp))) {
            drawRect(PieceMapColors.Missing)
            if (snapshot.cells.isNotEmpty()) {
                val step = size.width / snapshot.cells.size
                val gap = minOf(1.dp.toPx(), step * 0.2f)
                snapshot.cells.forEachIndexed { index, cell ->
                    drawRect(PieceMapColors.of(cell), Offset(index * step, 0f), Size(step - gap, size.height))
                }
            }
        }
        if (!compact) Row(Modifier.fillMaxWidth().then(if (inlineInfo) Modifier.horizontalScroll(rememberScrollState()) else Modifier),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (inlineInfo) Text(if (snapshot.total == 0) stringResource(R.string.piece_map_pending)
                else stringResource(R.string.piece_map_inline, snapshot.verified, snapshot.total),
                color = labelColor, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            PieceLegend(stringResource(R.string.label_complete), PieceMapColors.Verified, labelColor)
            PieceLegend(stringResource(R.string.piece_legend_low), PieceMapColors.Low, labelColor)
            PieceLegend(stringResource(R.string.piece_legend_medium), PieceMapColors.Medium, labelColor)
            PieceLegend(stringResource(R.string.piece_legend_high), PieceMapColors.High, labelColor)
            PieceLegend(stringResource(R.string.label_incomplete), PieceMapColors.Missing, labelColor)
        }
        if (!inlineInfo && showInfo) Text(when {
            snapshot.total == 0 -> stringResource(R.string.piece_map_pending)
            snapshot.total > snapshot.cells.size ->
                stringResource(R.string.piece_map_info_grouped, snapshot.verified, snapshot.total)
            else -> stringResource(R.string.piece_map_info, snapshot.verified, snapshot.total)
        },
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
