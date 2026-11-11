package com.example.torrentstreamplayer
import com.example.torrentstreamplayer.ui.SeekUiState
import org.junit.Assert.*
import org.junit.Test

class SeekUiStateTest {
    @Test fun jumpingPastTargetCompletesSeekInBothDirections() {
        assertFalse(SeekUiState().requested(30_000, 90_000, 10_000).timeChanged(32_000).waiting)
        assertFalse(SeekUiState().requested(30_000, 90_000, 60_000).timeChanged(28_000).waiting)
        assertTrue(SeekUiState().requested(30_000, 90_000, 60_000).timeChanged(61_000).waiting)
    }
    @Test fun ordinaryBufferingIsNotLabeledAsSeeking() {
        val buffering = SeekUiState().buffered(20f)
        assertEquals(R.string.player_waiting_buffering, buffering.waitingMessage)
        val seeking = buffering.requested(30_000, 90_000)
        assertEquals(R.string.player_waiting_seek, seeking.waitingMessage)
        assertEquals(R.string.player_waiting_buffering, seeking.timeChanged(30_000).buffered(20f).waitingMessage)
    }
    @Test fun oldPlaybackEventsDoNotClearSeekBuffering() {
        val state = SeekUiState().requested(60_000, 120_000)
        assertTrue(state.timeChanged(10_000).waiting)
        assertTrue(state.buffered(100f).waiting)
        assertFalse(state.timeChanged(60_300).waiting)
    }
    @Test fun dataBufferingRemainsUntilVlcReportsReady() {
        val state = SeekUiState().requested(30_000, 90_000).buffered(20f).timeChanged(30_000)
        assertTrue(state.waiting)
        assertFalse(state.buffered(100f).waiting)
    }
    @Test fun quickNewSeekReplacesPreviousTarget() {
        val state = SeekUiState().requested(10_000, 90_000).requested(70_000, 90_000)
        assertTrue(state.timeChanged(10_000).waiting)
        assertFalse(state.timeChanged(70_000).waiting)
    }
    @Test fun targetIsClampedToVideoBounds() {
        assertEquals(0L, SeekUiState().requested(-1000, 90_000).targetMs)
        assertEquals(90_000L, SeekUiState().requested(100_000, 90_000).targetMs)
    }
}
