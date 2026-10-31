package com.example.torrentstreamplayer.ui
import androidx.compose.ui.graphics.Color
import org.junit.Assert.*
import org.junit.Test
class DownloadMetricTextTest {
    @Test fun highlightsReceivedBytesPercentageAndSpeedSeparately() {
        val result = DownloadMetricText.build(100, 25, 2048, Color.Blue, Color.Gray, Color.Magenta) { "$it B" }
        assertEquals(DownloadProgressLabel.text(100, 25) { "$it B" } + " · 2048 B/s", result.text)
        val highlighted = result.spanStyles.filter { it.item.color == Color.Blue }.map { result.text.substring(it.start, it.end) }
        assertEquals(listOf("25 B", String.format(java.util.Locale.getDefault(), "%.1f%%", 25.0)), highlighted)
        val speed = result.spanStyles.single { it.item.color == Color.Magenta }
        assertEquals("2048 B/s", result.text.substring(speed.start, speed.end))
    }
    @Test fun unknownValuesRemainMutedAndNoSpeedIsInvented() {
        val result = DownloadMetricText.build(null, null, null, Color.Blue, Color.Gray, Color.Magenta) { "$it B" }
        assertEquals("— / — · 확인 전", result.text)
        assertEquals(1, result.spanStyles.size)
        assertEquals(Color.Gray, result.spanStyles.single().item.color)
    }
    @Test fun completedValuesCanUseSuccessColorWithoutChangingData() {
        val result = DownloadMetricText.build(100, 100, null, Color.Green, Color.Gray, Color.Magenta) { "$it B" }
        assertTrue(result.spanStyles.filter { it.item.color == Color.Green }.size == 2)
        assertFalse(result.text.contains("/s"))
    }
}
