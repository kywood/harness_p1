package com.example.torrentstreamplayer
import com.example.torrentstreamplayer.ui.playbackTimeLabel
import org.junit.Assert.assertEquals
import org.junit.Test
class PlaybackTimeLabelTest {
    @Test fun currentAndTotalAreClearlyDistinguished() {
        assertEquals("현재 12:34 / 전체 1:30:00", playbackTimeLabel(754000, 5400000))
    }
    @Test fun unknownTimesNeverShowNegativeValues() {
        assertEquals("현재 00:00 / 전체 00:00", playbackTimeLabel(-1, 0))
    }
    @Test fun seekCursorUsesRequestedPositionRatherThanOldPlaybackTime() {
        assertEquals("현재 1:02:03 / 전체 2:00:00", playbackTimeLabel(3723000, 7200000))
    }
}
