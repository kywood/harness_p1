package com.example.torrentstreamplayer.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import com.example.torrentstreamplayer.ui.SeparatedLinePlan

/**
 * 항목 [count]개를 가운데 맞춰 줄 단위로 놓고, 같은 줄 항목 사이에만 [separator]를 넣는다. 줄이 바뀌는 자리에는
 * 구분점이 남지 않는다([SeparatedLinePlan]). 각 줄 안에서 항목과 구분점은 세로 가운데 맞춤이다.
 *
 * @param separator 구분점 하나. 항목 사이마다 한 번씩(`count - 1`번) 그린다.
 * @param item 번호별 항목 하나.
 */
@Composable
internal fun SeparatedFlowRow(
    count: Int,
    separator: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    item: @Composable (Int) -> Unit,
) {
    Layout(
        contents = listOf({ repeat(count) { item(it) } }, { repeat((count - 1).coerceAtLeast(0)) { separator() } }),
        modifier = modifier,
    ) { (itemMeasurables, separatorMeasurables), constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val items = itemMeasurables.map { it.measure(loose) }
        val separators = separatorMeasurables.map { it.measure(loose) }
        val separatorWidth = separators.firstOrNull()?.width ?: 0
        val widths = items.map { it.width }
        val maxWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else Constraints.Infinity
        val lines = SeparatedLinePlan.lines(widths, separatorWidth, maxWidth)
        val lineHeights = lines.map { line ->
            line.maxOf { index -> maxOf(items[index].height, if (index > line.first) separators[index - 1].height else 0) }
        }
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth
            else lines.maxOfOrNull { SeparatedLinePlan.width(it, widths, separatorWidth) } ?: 0
        val height = lineHeights.sum().coerceIn(constraints.minHeight, constraints.maxHeight)
        layout(width.coerceAtLeast(constraints.minWidth), height) {
            var y = 0
            lines.forEachIndexed { lineIndex, line ->
                val lineHeight = lineHeights[lineIndex]
                var x = (width - SeparatedLinePlan.width(line, widths, separatorWidth)).coerceAtLeast(0) / 2
                line.forEach { index ->
                    if (index > line.first) {
                        val dot = separators[index - 1]
                        dot.placeRelative(x, y + (lineHeight - dot.height) / 2)
                        x += separatorWidth
                    }
                    val placeable = items[index]
                    placeable.placeRelative(x, y + (lineHeight - placeable.height) / 2)
                    x += placeable.width
                }
                y += lineHeight
            }
        }
    }
}
