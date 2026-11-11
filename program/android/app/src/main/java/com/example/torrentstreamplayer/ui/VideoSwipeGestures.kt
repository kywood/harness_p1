package com.example.torrentstreamplayer.ui

import androidx.compose.foundation.gestures.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
import kotlin.math.abs

internal suspend fun PointerInputScope.detectVideoSwipeGestures(
    canSeek: () -> Boolean, canPan: () -> Boolean,
    onStart: () -> Unit, onMove: (Float) -> Unit, onEnd: (Boolean) -> Unit,
    onTransform: (Offset, Offset, Float) -> Unit
) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        var total = Offset.Zero
        var swiping = false
        var multiple = false
        var cancelled = false
        do {
            val event = awaitPointerEvent()
            if (event.changes.any { it.isConsumed }) { cancelled = true; break }
            val pan = event.calculatePan()
            total += pan
            if (event.changes.count { it.pressed } > 1) {
                multiple = true
                if (swiping) { onEnd(false); swiping = false }
            }
            when {
                multiple -> {
                    onTransform(event.calculateCentroid(useCurrent = false), pan, event.calculateZoom())
                    event.changes.filter { it.positionChanged() }.forEach { it.consume() }
                }
                swiping -> {
                    onMove(total.x)
                    event.changes.filter { it.positionChanged() }.forEach { it.consume() }
                }
                total.getDistance() > viewConfiguration.touchSlop -> {
                    if (canSeek() && abs(total.x) > abs(total.y)) {
                        swiping = true; onStart(); onMove(total.x)
                    } else if (canPan()) onTransform(event.calculateCentroid(useCurrent = false), pan, 1f)
                    event.changes.filter { it.positionChanged() }.forEach { it.consume() }
                }
            }
        } while (event.changes.any { it.pressed })
        if (swiping) onEnd(!cancelled)
    }
}
