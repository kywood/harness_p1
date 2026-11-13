package com.example.torrentstreamplayer.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackClockTest {
    @Test fun countsOnlyWhileRunning() {
        var wall = 1_000L
        val clock = PlaybackClock { wall }
        wall += 500
        assertEquals("stopped at first", 0L, clock.elapsedMs())
        clock.setRunning(true)
        wall += 2_000
        assertEquals(2_000L, clock.elapsedMs())
        clock.setRunning(false)
        wall += 10_000
        assertEquals("buffering or paused does not count", 2_000L, clock.elapsedMs())
        clock.setRunning(true)
        clock.setRunning(true)
        wall += 1_000
        assertEquals(3_000L, clock.elapsedMs())
    }
}
