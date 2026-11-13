package com.example.torrentstreamplayer.ui

import com.example.torrentstreamplayer.ui.navigation.AppScreen
import org.junit.Assert.*
import org.junit.Test

class TestAdPolicyTest {
    @Test fun bannerIdMatchesBuildType() {
        assertEquals("ca-app-pub-3940256099942544/9214589741", TestAdPolicy.bannerUnitId(true))
        assertEquals("ca-app-pub-2418522550325553/6125182266", TestAdPolicy.bannerUnitId(false))
    }
    @Test fun bannerAlwaysUsesGoogleDemoUnit() {
        assertEquals("ca-app-pub-3940256099942544/9214589741", TestAdPolicy.BANNER_UNIT_ID)
    }
    @Test fun mainAndPlaylistHaveBannerButFilesDoNot() {
        assertTrue(TestAdPolicy.visible(AppScreen.Main()))
        assertTrue(TestAdPolicy.visible(AppScreen.Main("오류")))
        assertTrue(TestAdPolicy.visible(AppScreen.Playlist))
        assertFalse(TestAdPolicy.visible(AppScreen.StoredFiles))
        assertFalse(TestAdPolicy.visible(AppScreen.OpenSourceLicenses))
        assertFalse(TestAdPolicy.visible(AppScreen.Settings))
    }
    @Test fun preparationBufferingAndPlaybackHaveNoBanner() {
        listOf(AppScreen.Preparing.FetchingMetadata,
            AppScreen.Preparing.Buffering(StreamUiState.Buffering("video", 100, 0f, 0)),
            AppScreen.Player("video")).forEach {
            assertFalse(TestAdPolicy.visible(it))
        }
    }
}
