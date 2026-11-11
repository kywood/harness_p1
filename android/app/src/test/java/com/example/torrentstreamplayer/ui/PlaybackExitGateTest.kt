package com.example.torrentstreamplayer.ui
import org.junit.Assert.*
import org.junit.Test
class PlaybackExitGateTest {
    @Test fun backAndEndEventsCountOnlyOncePerPlayback() {
        val gate = PlaybackExitGate()
        assertTrue(gate.consume())
        assertFalse(gate.consume())
        assertTrue(PlaybackExitGate().consume())
    }
    @Test fun fiveShortPlaybackExitsQualifyForAnAd() {
        var count = 0
        repeat(5) {
            if (PlaybackExitGate().consume()) count = CompletionAdPolicy.nextCompletionCount(count)
        }
        assertTrue(CompletionAdPolicy.eligible(1000, 900000, null, count))
    }
}
