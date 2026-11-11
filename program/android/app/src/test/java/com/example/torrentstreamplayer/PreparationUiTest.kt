package com.example.torrentstreamplayer

import com.example.torrentstreamplayer.engine.MetadataDetails
import com.example.torrentstreamplayer.ui.AppBackPolicy
import com.example.torrentstreamplayer.ui.StreamUiState
import org.junit.Assert.*
import org.junit.Test

class PreparationUiTest {
    @Test fun metadataIsUnknownUntilReceivedRatherThanShowingFakePieces() {
        val initial = MetadataDetails()
        assertFalse(initial.hasMetadata)
        assertEquals(0, initial.pieceCount)
        assertNull(initial.name)
    }
    @Test fun receivedMetadataRetainsFileAndPieceDetails() {
        val details = MetadataDetails(source = "네트워크").connected(5).received("Sample", 3, 1000, 10, 100)
        assertTrue(details.hasMetadata)
        assertEquals("Sample", details.name)
        assertEquals(3, details.fileCount)
        assertEquals(1000L, details.totalSize)
        assertEquals(10, details.pieceCount)
        assertEquals(100L, details.pieceSize)
        assertEquals(5, details.peers)
    }
    @Test fun peerUpdatesDoNotDiscardMetadataAndNeverGoNegative() {
        val details = MetadataDetails().received("Sample", 1, 1000, 10, 100).connected(-1)
        assertTrue(details.hasMetadata)
        assertEquals(0, details.peers)
        assertEquals(10, details.pieceCount)
    }
    @Test fun activeStreamAndErrorsReturnToInputInsteadOfClosingActivity() {
        val states = listOf(StreamUiState.FetchingMetadata("magnet"),
            StreamUiState.Buffering("Sample", 1000, 0f, 0), StreamUiState.Playing("Sample"),
            StreamUiState.Error("Failed"))
        states.forEach { assertTrue(AppBackPolicy.returnsToInput(it, false, false)) }
    }
    @Test fun idleAndListsKeepTheirOwnBackBehavior() {
        assertFalse(AppBackPolicy.returnsToInput(StreamUiState.Idle, false, false))
        assertFalse(AppBackPolicy.returnsToInput(StreamUiState.Playing("Sample"), true, false))
        assertFalse(AppBackPolicy.returnsToInput(StreamUiState.Playing("Sample"), false, true))
    }
}
