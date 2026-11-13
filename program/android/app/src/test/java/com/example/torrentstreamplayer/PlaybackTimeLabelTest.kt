package com.example.torrentstreamplayer
import com.example.torrentstreamplayer.ui.formatTime
import org.junit.Assert.assertEquals
import org.junit.Test
class PlaybackTimeLabelTest {
    /** The player's time label template from [folder], formatted as the screen does. */
    private fun label(currentMs: Long, durationMs: Long, folder: String = "values-ko"): String {
        val template = StringsXml.texts(folder).getValue("player_time_label")
        return String.format(template, formatTime(currentMs), formatTime(durationMs))
    }
    @Test fun currentAndTotalAreClearlyDistinguished() {
        assertEquals("현재 12:34 / 전체 1:30:00", label(754000, 5400000))
        assertEquals("12:34 / 1:30:00", label(754000, 5400000, StringsXml.DEFAULT))
    }
    @Test fun unknownTimesNeverShowNegativeValues() {
        assertEquals("현재 00:00 / 전체 00:00", label(-1, 0))
        assertEquals("00:00 / 00:00", label(-1, 0, StringsXml.DEFAULT))
    }
    @Test fun seekCursorUsesRequestedPositionRatherThanOldPlaybackTime() {
        assertEquals("현재 1:02:03 / 전체 2:00:00", label(3723000, 7200000))
    }
}
