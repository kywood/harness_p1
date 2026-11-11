package com.example.torrentstreamplayer.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackResumeStateTest {
    private val first = "magnet:?xt=urn:btih:" + "a".repeat(40)
    private val second = "magnet:?xt=urn:btih:" + "b".repeat(40)

    @Test fun recreatedPlayerResumesFromLastSavedPositionNotFromWhereItOpened() {
        val state = PlaybackResumeState()
        state.start(first, 10_000)
        state.saved(first, 2_400_000, 3_600_000)
        assertEquals(2_400_000, state.positionMs)
    }

    @Test fun lateSaveFromPreviousItemDoesNotMoveCurrentItem() {
        val state = PlaybackResumeState()
        state.start(second, 5_000)
        state.saved(first, 900_000, 3_600_000)
        assertEquals(5_000, state.positionMs)
    }

    @Test fun finishedOrInvalidPositionsResumeFromStart() {
        val state = PlaybackResumeState()
        state.start(first, -1)
        assertEquals(0, state.positionMs)
        state.saved(first, 3_599_000, 3_600_000)
        assertEquals(0, state.positionMs)
        state.saved(first, -5, -1)
        assertEquals(0, state.positionMs)
    }

    @Test fun nothingPlaysBeforeStart() {
        assertNull(PlaybackResumeState().magnet)
    }
}
