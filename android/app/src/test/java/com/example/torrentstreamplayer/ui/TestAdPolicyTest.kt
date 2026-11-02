package com.example.torrentstreamplayer.ui

import org.junit.Assert.*
import org.junit.Test

class TestAdPolicyTest {
    @Test fun bannerAlwaysUsesGoogleDemoUnit() {
        assertEquals("ca-app-pub-3940256099942544/9214589741", TestAdPolicy.BANNER_UNIT_ID)
    }
    @Test fun mainAndPlaylistHaveBannerButFilesDoNot() {
        assertTrue(TestAdPolicy.visible(StreamUiState.Idle, false, false))
        assertTrue(TestAdPolicy.visible(StreamUiState.FetchingMetadata("test"), false, true))
        assertFalse(TestAdPolicy.visible(StreamUiState.Idle, true, false))
    }
    @Test fun preparationBufferingAndPlaybackHaveNoBanner() {
        listOf(StreamUiState.FetchingMetadata("test"),
            StreamUiState.Buffering("video", 100, 0f, 0),
            StreamUiState.Playing("video")).forEach {
            assertFalse(TestAdPolicy.visible(it, false, false))
        }
    }
}
