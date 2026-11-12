package com.example.torrentstreamplayer.ui.navigation

import com.example.torrentstreamplayer.ui.StreamUiState
import org.junit.Assert.assertEquals
import org.junit.Test

class AppNavigatorTest {
    private val buffering = StreamUiState.Buffering("video.mp4", 100, 10f, 0)

    @Test fun startsOnMain() {
        assertEquals(AppScreen.Main(), AppNavigator().screen.value)
    }

    @Test fun mainFollowsStreamState() {
        val navigator = AppNavigator()
        navigator.onStreamChanged(StreamUiState.FetchingMetadata("magnet"))
        assertEquals(AppScreen.Preparing.FetchingMetadata, navigator.screen.value)
        navigator.onStreamChanged(buffering)
        assertEquals(AppScreen.Preparing.Buffering(buffering), navigator.screen.value)
        navigator.onStreamChanged(StreamUiState.Playing("video.mp4"))
        assertEquals(AppScreen.Player("video.mp4"), navigator.screen.value)
        navigator.onStreamChanged(StreamUiState.Error("실패"))
        assertEquals(AppScreen.Main("실패"), navigator.screen.value)
    }

    @Test fun listsCoverStreamStateAndBackReturnsToMain() {
        val navigator = AppNavigator()
        navigator.openPlaylist()
        navigator.onStreamChanged(StreamUiState.Error("실패"))
        assertEquals(AppScreen.Playlist, navigator.screen.value)
        navigator.closeList()
        assertEquals(AppScreen.Main("실패"), navigator.screen.value)
        navigator.openStoredFiles()
        assertEquals(AppScreen.StoredFiles, navigator.screen.value)
        navigator.closeList()
        assertEquals(AppScreen.Main("실패"), navigator.screen.value)
        navigator.openOpenSourceLicenses()
        assertEquals(AppScreen.OpenSourceLicenses, navigator.screen.value)
        navigator.closeList()
        assertEquals(AppScreen.Main("실패"), navigator.screen.value)
    }

    @Test fun playbackFromPlaylistReturnsToPlaylistWhenClosed() {
        val navigator = AppNavigator()
        navigator.openPlaylist()
        navigator.startPlayback(PlaybackOrigin.PLAYLIST)
        navigator.onStreamChanged(StreamUiState.Playing("video.mp4"))
        assertEquals(AppScreen.Player("video.mp4"), navigator.screen.value)
        assertEquals(PlaybackOrigin.PLAYLIST, navigator.closePlayback())
        navigator.onStreamChanged(StreamUiState.Idle)
        assertEquals(AppScreen.Playlist, navigator.screen.value)
        assertEquals(PlaybackOrigin.MAIN, navigator.origin)
    }

    @Test fun playbackFromMainReturnsToMainWhenClosed() {
        val navigator = AppNavigator()
        navigator.startPlayback(PlaybackOrigin.MAIN)
        navigator.onStreamChanged(buffering)
        assertEquals(PlaybackOrigin.MAIN, navigator.closePlayback())
        navigator.onStreamChanged(StreamUiState.Idle)
        assertEquals(AppScreen.Main(), navigator.screen.value)
    }

    @Test fun originIsForgottenAfterClosingSoTheNextCloseGoesToMain() {
        val navigator = AppNavigator()
        navigator.startPlayback(PlaybackOrigin.PLAYLIST)
        navigator.closePlayback()
        navigator.closeList()
        navigator.onStreamChanged(StreamUiState.Error("실패"))
        assertEquals(PlaybackOrigin.MAIN, navigator.closePlayback())
        assertEquals(AppScreen.Main("실패"), navigator.screen.value)
    }

    @Test fun cancelFromPreparationGoesToMainEvenWhenStartedFromPlaylist() {
        val navigator = AppNavigator()
        navigator.startPlayback(PlaybackOrigin.PLAYLIST)
        navigator.onStreamChanged(buffering)
        navigator.cancelToMain()
        navigator.onStreamChanged(StreamUiState.Idle)
        assertEquals(AppScreen.Main(), navigator.screen.value)
        assertEquals(PlaybackOrigin.MAIN, navigator.origin)
    }

    @Test fun continuingInPlaylistOrCompletingPlaybackShowsPlaylistAndForgetsOrigin() {
        val navigator = AppNavigator()
        navigator.startPlayback(PlaybackOrigin.PLAYLIST)
        navigator.onStreamChanged(StreamUiState.Playing("video.mp4"))
        navigator.showPlaylistAfterPlayback()
        assertEquals(AppScreen.Playlist, navigator.screen.value)
        assertEquals(PlaybackOrigin.MAIN, navigator.origin)
        navigator.closeList()
        navigator.onStreamChanged(StreamUiState.Idle)
        assertEquals(AppScreen.Main(), navigator.screen.value)
    }

    @Test fun startingPlaybackLeavesTheList() {
        val navigator = AppNavigator()
        navigator.openPlaylist()
        navigator.startPlayback(PlaybackOrigin.PLAYLIST)
        navigator.onStreamChanged(StreamUiState.FetchingMetadata("magnet"))
        assertEquals(AppScreen.Preparing.FetchingMetadata, navigator.screen.value)
    }
}
