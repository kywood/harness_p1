package com.example.torrentstreamplayer
import com.example.torrentstreamplayer.ui.SwipeSeek
import org.junit.Assert.*
import org.junit.Test

class SwipeSeekTest {
    @Test fun swipeRatioMatchesFullVideoTimelineInEitherOrientation() {
        assertEquals(960_000L, SwipeSeek.target(40f, 400f, 600_000, 3_600_000))
        assertEquals(240_000L, SwipeSeek.target(-80f, 800f, 600_000, 3_600_000))
        assertEquals(3_600_000L, SwipeSeek.target(400f, 400f, 600_000, 3_600_000))
    }
    @Test fun horizontalDistancePreviewsSeekRelativeToGestureStart() {
        assertEquals(60_000L, SwipeSeek.target(100f, 400f, 30_000, 120_000))
        assertEquals(0L, SwipeSeek.target(-100f, 400f, 30_000, 120_000))
        assertEquals(30_000L, SwipeSeek.target(0f, 400f, 30_000, 120_000))
    }
    @Test fun shortVideosAndBothEndsAreClamped() {
        assertEquals(15_000L, SwipeSeek.target(100f, 400f, 10_000, 20_000))
        assertEquals(0L, SwipeSeek.target(-400f, 400f, 5000, 120_000))
        assertEquals(120_000L, SwipeSeek.target(400f, 400f, 119_000, 120_000))
    }
    @Test fun invalidGeometryDoesNotSeekAndHugeValuesDoNotOverflow() {
        assertNull(SwipeSeek.target(10f, 0f, 10_000, 120_000))
        assertNull(SwipeSeek.target(Float.NaN, 400f, 10_000, 120_000))
        assertNull(SwipeSeek.target(10f, 400f, 10_000, 0))
        assertEquals(Long.MAX_VALUE, SwipeSeek.target(400f, 400f, Long.MAX_VALUE - 1, Long.MAX_VALUE))
    }
}
