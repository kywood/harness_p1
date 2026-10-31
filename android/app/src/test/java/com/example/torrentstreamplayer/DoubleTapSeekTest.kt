package com.example.torrentstreamplayer
import com.example.torrentstreamplayer.ui.DoubleTapSeek
import org.junit.Assert.*
import org.junit.Test

class DoubleTapSeekTest {
    @Test fun leftAndRightMoveTenSecondsFromPlaybackPosition() {
        assertEquals(20_000L, DoubleTapSeek.target(20f, 400f, 30_000, 100_000))
        assertEquals(40_000L, DoubleTapSeek.target(380f, 400f, 30_000, 100_000))
    }
    @Test fun seeksStopAtVideoStartAndEnd() {
        assertEquals(0L, DoubleTapSeek.target(0f, 400f, 5000, 100_000))
        assertEquals(100_000L, DoubleTapSeek.target(400f, 400f, 99_000, 100_000))
    }
    @Test fun repeatedDoubleTapsAccumulateFromPendingTarget() {
        val first = DoubleTapSeek.target(300f, 400f, 30_000, 100_000)!!
        assertEquals(50_000L, DoubleTapSeek.target(300f, 400f, first, 100_000))
        assertEquals(30_000L, DoubleTapSeek.target(20f, 400f, first, 100_000))
    }
    @Test fun rotationUsesCurrentWidthAndCenterBelongsToRightSide() {
        assertEquals(20_000L, DoubleTapSeek.target(300f, 1000f, 30_000, 100_000))
        assertEquals(40_000L, DoubleTapSeek.target(300f, 400f, 30_000, 100_000))
        assertEquals(40_000L, DoubleTapSeek.target(200f, 400f, 30_000, 100_000))
    }
    @Test fun unknownDurationAndInvalidBoundsDoNotSeek() {
        assertNull(DoubleTapSeek.target(10f, 0f, 30_000, 100_000))
        assertNull(DoubleTapSeek.target(10f, 400f, 30_000, 0))
        assertNull(DoubleTapSeek.target(Float.NaN, 400f, 30_000, 100_000))
        assertNull(DoubleTapSeek.target(10f, Float.POSITIVE_INFINITY, 30_000, 100_000))
    }
    @Test fun largeTimeValuesCannotOverflow() {
        assertEquals(Long.MAX_VALUE, DoubleTapSeek.target(300f, 400f, Long.MAX_VALUE - 1, Long.MAX_VALUE))
    }
}
