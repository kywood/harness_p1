package com.example.torrentstreamplayer

import androidx.compose.ui.graphics.luminance
import com.example.torrentstreamplayer.ui.theme.PlaybackColors
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackColorsTest {
    @Test fun playbackBackgroundsKeepVideoVisible() {
        org.junit.Assert.assertEquals(0.45f, PlaybackColors.SeekPanel.alpha, 0.005f)
        org.junit.Assert.assertEquals(0.15f, PlaybackColors.ControlsScrim.alpha, 0.005f)
        val combinedOpacity = 1f - (1f - PlaybackColors.SeekPanel.alpha) *
            (1f - PlaybackColors.ControlsScrim.alpha)
        assertTrue("Video must remain visible through both layers", combinedOpacity < 0.55f)
    }
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
