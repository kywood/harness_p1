package com.example.torrentstreamplayer.ui

import com.example.torrentstreamplayer.ui.navigation.AppScreen
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSnackbarPolicyTest {
    @Test fun listScreensShowTheSharedSnackbar() {
        listOf(AppScreen.Main(), AppScreen.Main("오류"), AppScreen.Playlist, AppScreen.StoredFiles,
            AppScreen.OpenSourceLicenses, AppScreen.Settings).forEach { assertTrue(AppSnackbarPolicy.visible(it)) }
    }

    @Test fun preparationAndPlaybackNeverShowIt() {
        listOf(AppScreen.Preparing.FetchingMetadata,
            AppScreen.Preparing.Buffering(StreamUiState.Buffering("video", 100, 0f, 0)),
            AppScreen.Player("video")).forEach { assertFalse(AppSnackbarPolicy.visible(it)) }
    }
}
