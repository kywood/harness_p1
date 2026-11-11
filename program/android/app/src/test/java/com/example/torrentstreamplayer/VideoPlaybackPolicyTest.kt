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
        assertTrue(VideoPlaybackPolicy.options.contains("--drop-late-frames"))
        assertTrue(VideoPlaybackPolicy.options.contains("--skip-frames"))
        assertFalse(VideoPlaybackPolicy.options.contains("--no-drop-late-frames"))
        assertFalse(VideoPlaybackPolicy.options.contains("--no-skip-frames"))
    }
}
