package com.example.torrentstreamplayer

import com.example.torrentstreamplayer.engine.MetadataDetails
import com.example.torrentstreamplayer.ui.AppBackPolicy
import com.example.torrentstreamplayer.ui.StreamUiState
import com.example.torrentstreamplayer.ui.navigation.AppScreen
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
        val screens = listOf(AppScreen.Preparing.FetchingMetadata,
            AppScreen.Preparing.Buffering(StreamUiState.Buffering("Sample", 1000, 0f, 0)), AppScreen.Player("Sample"),
            AppScreen.Main("Failed"))
        screens.forEach { assertTrue(AppBackPolicy.returnsToInput(it)) }
    }
    @Test fun idleAndListsKeepTheirOwnBackBehavior() {
        assertFalse(AppBackPolicy.returnsToInput(AppScreen.Main()))
        assertFalse(AppBackPolicy.returnsToInput(AppScreen.StoredFiles))
        assertFalse(AppBackPolicy.returnsToInput(AppScreen.OpenSourceLicenses))
        assertFalse(AppBackPolicy.returnsToInput(AppScreen.Playlist))
    }
}
