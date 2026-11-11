package com.example.torrentstreamplayer.ui
import org.junit.Assert.assertEquals
import org.junit.Test
class DownloadSpeedLabelTest {
    @Test fun inactiveDownloadHasNoSpeed() { assertEquals("", DownloadProgressLabel.speed(null) { "$it B" }) }
    @Test fun activeDownloadShowsLiveRate() { assertEquals(" · 2048 B/s", DownloadProgressLabel.speed(2048) { "$it B" }) }
    @Test fun stalledOrInvalidRateShowsZero() {
        for (rate in listOf(0L, -1L)) assertEquals(" · 0 B/s", DownloadProgressLabel.speed(rate) { "$it B" })
    }
}
