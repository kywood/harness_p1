package com.example.torrentstreamplayer

import com.example.torrentstreamplayer.ui.VideoZoomState
import org.junit.Assert.*
import org.junit.Test

class VideoZoomStateTest {
    @Test fun pinchScalesProportionallyAndCanBeReversed() {
        val zoomed = VideoZoomState().transformed(1.5f, 0f, 0f, 1000f, 600f)
        assertEquals(1.5f, zoomed.scale, 0.001f)
        assertEquals(1f, zoomed.transformed(1f / 1.5f, 0f, 0f, 1000f, 600f).scale, 0.001f)
    }
    @Test fun scaleIsBoundedAndFitResetsPan() {
        val zoomed = VideoZoomState().transformed(100f, 100f, 100f, 1000f, 600f)
        assertEquals(4f, zoomed.scale, 0f)
        val fit = zoomed.transformed(0.01f, 0f, 0f, 1000f, 600f)
        assertEquals(1f, fit.scale, 0f)
        assertEquals(0f, fit.x, 0f)
        assertEquals(0f, fit.y, 0f)
    }
    @Test fun panCannotExposeOutsideVideoViewport() {
        val zoomed = VideoZoomState().transformed(2f, 9999f, -9999f, 1000f, 600f)
        assertEquals(500f, zoomed.x, 0f)
        assertEquals(-300f, zoomed.y, 0f)
    }
    @Test fun pinchKeepsOffCenterFingerFocusStable() {
        val zoomed = VideoZoomState().transformed(2f, 0f, 0f, 1000f, 600f, 750f, 300f)
        assertEquals(-250f, zoomed.x, 0f)
        assertEquals(0f, zoomed.y, 0f)
    }
    @Test fun emulatorButtonsUseSameZoomStateAsPinch() {
        val plus = VideoZoomState().transformed(1.25f, 0f, 0f, 1000f, 600f)
        assertEquals(1.25f, plus.scale, 0f)
        assertEquals(VideoZoomState(), plus.transformed(0.8f, 0f, 0f, 1000f, 600f))
    }
    @Test fun invalidGestureAndUnmeasuredViewportAreIgnored() {
        val state = VideoZoomState()
        listOf(Float.NaN, Float.POSITIVE_INFINITY, -1f, 0f).forEach {
            assertEquals(state, state.transformed(it, 0f, 0f, 1000f, 600f))
        }
        assertEquals(state, state.transformed(2f, 0f, 0f, 0f, 0f))
    }
}
