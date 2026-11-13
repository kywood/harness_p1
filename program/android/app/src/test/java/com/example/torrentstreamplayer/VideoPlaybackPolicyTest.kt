package com.example.torrentstreamplayer
import com.example.torrentstreamplayer.ui.VideoPlaybackPolicy
import org.junit.Assert.*
import org.junit.Test

class VideoPlaybackPolicyTest {
    @Test fun brokenEmulatorCodecUsesSoftwareDecoding() {
        assertFalse(VideoPlaybackPolicy.hardwareDecoding("ranchu"))
        assertFalse(VideoPlaybackPolicy.hardwareDecoding("goldfish"))
        assertFalse(VideoPlaybackPolicy.hardwareDecoding("RANCHU"))
    }
    @Test fun physicalDeviceRetainsHardwareDecoding() {
        assertTrue(VideoPlaybackPolicy.hardwareDecoding("exynos7885"))
        assertTrue(VideoPlaybackPolicy.hardwareDecoding("qcom"))
    }
    @Test fun lateFramesCanBeSkippedToCatchUpWithPlayback() {
        listOf(true, false).forEach { debug ->
            val options = VideoPlaybackPolicy.options(debug)
            assertTrue(options.contains("--drop-late-frames"))
            assertTrue(options.contains("--skip-frames"))
            assertFalse(options.contains("--no-drop-late-frames"))
            assertFalse(options.contains("--no-skip-frames"))
        }
    }
    @Test fun noSubtitleEncodingIsForcedOnVlc() {
        assertFalse(VideoPlaybackPolicy.options(debug = false).any { it.startsWith("--subsdec-encoding") })
    }
    @Test fun vlcDoesNotAddSubtitleFilesOnItsOwn() {
        listOf(true, false).forEach { debug ->
            val options = VideoPlaybackPolicy.options(debug)
            assertTrue(options.contains("--no-sub-autodetect-file"))
            assertFalse(options.contains("--sub-autodetect-file"))
        }
    }
    @Test fun verboseNativeLoggingIsOnlyForDebugBuilds() {
        assertTrue(VideoPlaybackPolicy.options(debug = true).contains("-vvv"))
        assertFalse(VideoPlaybackPolicy.options(debug = false).contains("-vvv"))
    }
}
