package com.example.torrentstreamplayer.ui.player

import com.example.torrentstreamplayer.engine.PieceCell
import com.example.torrentstreamplayer.engine.PieceMapSnapshot
import org.junit.Assert.assertEquals
import org.junit.Test

class SeekTrackPlanTest {
    @Test fun onlyFullyVerifiedCellsArePaintedAsDownloaded() {
        assertEquals(listOf(PieceCell.VERIFIED), PieceCell.entries.filter(SeekTrackPlan::downloaded))
    }

    @Test fun partlyDownloadedCellStaysInTheTrackColor() {
        // 4 cells of 25 pieces: all, 24 of 25, 1 of 25, none.
        val pieces = PieceMapSnapshot.capture(0, 99, 4) { it < 49 || it == 50 }
        assertEquals(listOf(PieceCell.VERIFIED, PieceCell.HIGH, PieceCell.LOW, PieceCell.MISSING), pieces.cells)
        assertEquals(listOf(true, false, false, false), pieces.cells.map(SeekTrackPlan::downloaded))
    }
}
