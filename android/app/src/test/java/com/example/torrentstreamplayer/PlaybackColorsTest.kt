package com.example.torrentstreamplayer

import androidx.compose.ui.graphics.luminance
import com.example.torrentstreamplayer.ui.theme.PlaybackColors
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackColorsTest {
    @Test fun progressIndicatorsHaveAccessibleContrastAgainstTrack() {
        listOf(PlaybackColors.Accent, PlaybackColors.Download, PlaybackColors.PiecePartial).forEach { color ->
            val contrast = (color.luminance() + 0.05f) / (PlaybackColors.Track.luminance() + 0.05f)
            assertTrue("Progress contrast must be at least 3:1", contrast >= 3f)
        }
    }
    @Test fun downloadLabelHasReadableContrastAgainstPanel() {
        val contrast = (PlaybackColors.Download.luminance() + 0.05f) / (PlaybackColors.Panel.luminance() + 0.05f)
        assertTrue("Label contrast must be at least 4.5:1", contrast >= 4.5f)
    }
}
