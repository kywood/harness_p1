package com.example.torrentstreamplayer
import com.example.torrentstreamplayer.ui.DownloadProgressLabel
import org.junit.Assert.*
import org.junit.Test

class DownloadProgressLabelTest {
    @Test fun allStatisticsFitOneLine() {
        val text = DownloadProgressLabel.text(1000, 250, "확인 전") { "${it}B" }
        assertEquals("250B / 1000B · " + String.format(java.util.Locale.getDefault(), "%.1f%%", 25.0), text)
        assertFalse(text.contains("\n"))
    }
    @Test fun unknownAndPartialMetadataDoNotPretendToBeZeroPercent() {
        assertEquals("— / — · 확인 전", DownloadProgressLabel.text(null, null, "확인 전") { "${it}B" })
        assertEquals("— / 1000B · 확인 전", DownloadProgressLabel.text(1000, null, "확인 전") { "${it}B" })
        assertEquals("— / — · 확인 전", DownloadProgressLabel.text(0, 0, "확인 전") { "${it}B" })
    }
    @Test fun downloadedSizeIsClampedToFileBounds() {
        assertTrue(DownloadProgressLabel.text(1000, 2000, "확인 전") { "${it}B" }.startsWith("1000B / 1000B"))
        assertTrue(DownloadProgressLabel.text(1000, -5, "확인 전") { "${it}B" }.startsWith("0B / 1000B"))
    }
}
