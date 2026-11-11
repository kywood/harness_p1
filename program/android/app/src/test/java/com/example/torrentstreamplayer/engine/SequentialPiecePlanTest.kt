package com.example.torrentstreamplayer.engine
import org.junit.Assert.*
import org.junit.Test

class SequentialPiecePlanTest {
    @Test fun completedWindowTransfersUrgencyAndKeepsBackgroundEnabled() {
        val next = SequentialPiecePlan.nextMissing(500, 999) { it <= 503 }!!
        val forward = SequentialPiecePlan.window(next, 999)
        assertEquals(1, SequentialPiecePlan.priority(500, forward))
        assertEquals(7, SequentialPiecePlan.priority(504, forward))
        val backward = SequentialPiecePlan.window(100, 999)
        assertEquals(1, SequentialPiecePlan.priority(504, backward))
        assertEquals(7, SequentialPiecePlan.priority(100, backward))
    }
    @Test fun seekingKeepsEveryPieceEnabledButOnlyTargetWindowUrgent() {
        val window = SequentialPiecePlan.window(500, 999)
        val enabled = (0..999).filter { SequentialPiecePlan.priority(it, window) == 7 }
        assertEquals(listOf(500, 501, 502, 503), enabled)
        assertEquals(1, SequentialPiecePlan.priority(499, window))
        assertEquals(1, SequentialPiecePlan.priority(504, window))
        assertTrue((0..999).all { SequentialPiecePlan.priority(it, window) > 0 })
    }
    @Test fun missingSeekPieceIsNotSkippedAndCompletedTailStops() {
        assertEquals(500, SequentialPiecePlan.nextMissing(500, 999) { it != 500 })
        assertEquals(503, SequentialPiecePlan.nextMissing(500, 999) { it < 503 })
        assertNull(SequentialPiecePlan.nextMissing(500, 999) { true })
        assertNull(SequentialPiecePlan.nextMissing(-1, 999) { false })
    }
    @Test fun completedSeekWindowContinuesWithoutAnotherPlayerRead() {
        val next = SequentialPiecePlan.nextMissing(500, 999) { it in 500..503 }
        assertEquals(504, next)
        assertEquals(listOf(504, 505, 506, 507),
            SequentialPiecePlan.requests(next!!, 999) { false }.map { it.piece })
    }
    @Test fun playbackAdvancesPriorityWindowFromSeekInsteadOfFileStart() {
        val atSeek = SequentialPiecePlan.requests(500, 999) { it == 500 }
        val advanced = SequentialPiecePlan.requests(501, 999) { it <= 502 }
        assertEquals(listOf(501, 502, 503), atSeek.map { it.piece })
        assertEquals(listOf(503, 504), advanced.map { it.piece })
        assertEquals(0, advanced.first().deadlineMs)
    }
    @Test fun earliestMissingPieceGetsHighestPriorityAndImmediateDeadline() {
        val plan = SequentialPiecePlan.requests(100, 999) { false }
        assertEquals(listOf(100, 101, 102, 103), plan.map { it.piece })
        assertEquals(listOf(0, 750, 1500, 2250), plan.map { it.deadlineMs })
        assertEquals(listOf(7, 6, 5, 4), plan.map { it.priority })
    }
    @Test fun alreadyDownloadedPiecesAreNotRequestedAgain() {
        val plan = SequentialPiecePlan.requests(10, 100) { it == 10 || it == 12 }
        assertEquals(listOf(11, 13), plan.map { it.piece })
        assertEquals(0, plan.first().deadlineMs)
        assertEquals(7, plan.first().priority)
    }
    @Test fun seekStartsAtNewPositionWithoutPrioritizingEarlierFileGap() {
        val plan = SequentialPiecePlan.requests(500, 999) { false }
        assertEquals(500, plan.first().piece)
        assertTrue(plan.all { it.piece in 500..503 })
    }
    @Test fun videoEndAndUnalignedSelectionStayInsidePieceRange() {
        assertEquals(42..43, SequentialPiecePlan.window(42, 43))
        assertEquals(listOf(43), SequentialPiecePlan.requests(43, 43) { false }.map { it.piece })
        assertTrue(SequentialPiecePlan.requests(43, 43) { true }.isEmpty())
    }
    @Test fun invalidAndExtremeRangesDoNotOverflow() {
        assertTrue(SequentialPiecePlan.window(-1, 10).isEmpty())
        assertTrue(SequentialPiecePlan.window(10, 9).isEmpty())
        assertEquals((Int.MAX_VALUE - 1)..Int.MAX_VALUE,
            SequentialPiecePlan.window(Int.MAX_VALUE - 1, Int.MAX_VALUE))
    }
}
