package com.example.torrentstreamplayer
import com.example.torrentstreamplayer.ui.formatTime
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
class PlaybackTimeLabelTest {
    /** The player's time label template from strings.xml, formatted as the screen does. */
    private fun label(currentMs: Long, durationMs: Long): String {
        val xml = File("src/main/res/values/strings.xml").readText()
        val template = Regex("<string name=\"player_time_label\">([^<]*)</string>").find(xml)!!.groupValues[1]
        return String.format(template, formatTime(currentMs), formatTime(durationMs))
    }
    @Test fun currentAndTotalAreClearlyDistinguished() {
        assertEquals("현재 12:34 / 전체 1:30:00", label(754000, 5400000))
    }
    @Test fun unknownTimesNeverShowNegativeValues() {
        assertEquals("현재 00:00 / 전체 00:00", label(-1, 0))
    }
    @Test fun seekCursorUsesRequestedPositionRatherThanOldPlaybackTime() {
        assertEquals("현재 1:02:03 / 전체 2:00:00", label(3723000, 7200000))
    }
}
