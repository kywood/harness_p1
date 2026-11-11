package com.example.torrentstreamplayer.engine
import org.junit.Assert.*
import org.junit.Test

class PieceMapSnapshotTest {
    @Test fun partialThresholdsUseVerifiedPieceCounts() {
        val states = listOf(0, 1, 33, 34, 66, 67, 99, 100).map { count ->
            PieceMapSnapshot.capture(0, 99, 1) { it < count }.cells.single()
        }
        assertEquals(listOf(PieceCell.MISSING, PieceCell.LOW, PieceCell.LOW, PieceCell.MEDIUM,
            PieceCell.MEDIUM, PieceCell.HIGH, PieceCell.HIGH, PieceCell.VERIFIED), states)
    }
    @Test fun holesRemainVisibleInsteadOfBecomingSolidPercentageBar() {
        val map = PieceMapSnapshot.capture(0, 3) { it == 0 || it == 2 }
        assertEquals(listOf(PieceCell.VERIFIED, PieceCell.MISSING,
            PieceCell.VERIFIED, PieceCell.MISSING), map.cells)
        assertEquals(2, map.verified)
        assertEquals(4, map.total)
    }
    @Test fun onlySelectedVideoPiecesAreCountedInTorrentOrder() {
        val visited = mutableListOf<Int>()
        val map = PieceMapSnapshot.capture(5, 8) { visited.add(it); it == 7 }
        assertEquals(listOf(5, 6, 7, 8), visited)
        assertEquals(listOf(PieceCell.MISSING, PieceCell.MISSING,
            PieceCell.VERIFIED, PieceCell.MISSING), map.cells)
        assertEquals(1, map.verified)
    }
    @Test fun groupedCellNeverClaimsCompletionWithMissingPieces() {
        val map = PieceMapSnapshot.capture(0, 7, 2) { it in 0..2 || it == 4 }
        assertEquals(listOf(PieceCell.HIGH, PieceCell.LOW), map.cells)
        assertEquals(4, map.verified)
        assertEquals(8, map.total)
    }
    @Test fun completeAndMissingGroupsHaveDistinctStates() {
        val map = PieceMapSnapshot.capture(0, 7, 2) { it < 4 }
        assertEquals(listOf(PieceCell.VERIFIED, PieceCell.MISSING), map.cells)
    }
    @Test fun unevenGroupsCoverEveryPieceExactlyOnce() {
        val visited = mutableListOf<Int>()
        val map = PieceMapSnapshot.capture(9, 19, 4) { visited.add(it); true }
        assertEquals((9..19).toList(), visited)
        assertEquals(11, map.total)
        assertEquals(11, map.verified)
        assertEquals(List(4) { PieceCell.VERIFIED }, map.cells)
    }
    @Test fun hugeTorrentProducesBoundedDrawingData() {
        val map = PieceMapSnapshot.capture(0, 9999) { it % 2 == 0 }
        assertEquals(256, map.cells.size)
        assertEquals(5000, map.verified)
        assertTrue(map.cells.all { it == PieceCell.MEDIUM })
    }
    @Test fun unknownMetadataIsEmptyAndSinglePieceIsSupported() {
        assertEquals(PieceMapSnapshot.EMPTY, PieceMapSnapshot.capture(-1, -1) { error("No query") })
        val map = PieceMapSnapshot.capture(7, 7) { true }
        assertEquals(listOf(PieceCell.VERIFIED), map.cells)
        assertEquals(1, map.total)
    }
    @Test fun invalidDisplayLimitIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { PieceMapSnapshot.capture(0, 4, 0) { true } }
    }
}
