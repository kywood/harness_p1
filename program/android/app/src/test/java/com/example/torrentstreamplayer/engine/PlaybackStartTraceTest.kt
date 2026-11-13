package com.example.torrentstreamplayer.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackStartTraceTest {
    private val key = "f30a2b7051e89bdf12fda6a1fc84474c76155025aa71193a9fb0bbbdb3d65c13"
    private var clockMs = 1_000L
    private fun trace(limit: Int = PlaybackStartTrace.DEFAULT_PIECE_LIMIT) =
        PlaybackStartTrace(key, { clockMs }, limit)

    @Test fun linesCarryOnlyTheShortenedKey() {
        val trace = trace()
        val have = mutableSetOf<Int>()
        val lines = listOfNotNull(
            trace.observe(0..3) { it in have },
            trace.playbackRequested(),
            trace.observe(0..3) { have += 0; it in have },
            trace.ready(),
        )
        assertEquals(4, lines.size)
        lines.forEach { line ->
            assertTrue(line, line.startsWith("[f30a2b70] "))
            assertFalse(line, line.contains(key.substring(8)))
            assertFalse(line, (8..key.length - 6).any { line.contains(key.substring(it, it + 6)) })
        }
    }

    @Test fun firstObservationCountsResumedPiecesInsteadOfAnOrder() {
        val trace = trace()
        assertEquals("[f30a2b70] resumed with 2/5 pieces", trace.observe(10..14) { it == 10 || it == 13 })
    }

    @Test fun newPiecesAreReportedRelativeToTheFileInFinishOrder() {
        val trace = trace()
        val have = mutableSetOf<Int>()
        trace.observe(10..14) { it in have }
        clockMs += 400
        have += 12
        assertEquals("[f30a2b70] +400ms finished pieces [2]", trace.observe(10..14) { it in have })
        have += setOf(10, 14)
        assertEquals("[f30a2b70] +400ms finished pieces [0, 4]", trace.observe(10..14) { it in have })
        assertNull(trace.observe(10..14) { it in have })
    }

    @Test fun pieceOrderStopsAtTheLimit() {
        val trace = trace(limit = 2)
        trace.observe(0..4) { false }
        assertEquals("[f30a2b70] +0ms finished pieces [0, 1]", trace.observe(0..4) { it < 3 })
        assertNull(trace.observe(0..4) { true })
    }

    @Test fun pastTheLimitPiecesAreNotCheckedAgain() {
        val trace = trace(limit = 1)
        trace.observe(0..4) { false }
        trace.observe(0..4) { it == 0 }
        var checks = 0
        repeat(5) { assertNull(trace.observe(0..4) { checks++; true }) }
        assertEquals(0, checks)
    }

    @Test fun readyReportsTimeSinceRequestOnce() {
        val trace = trace()
        assertNull(trace.ready())
        clockMs += 500
        trace.playbackRequested()
        trace.observe(0..2) { false }
        clockMs += 2_000
        trace.observe(0..2) { it == 0 }
        assertEquals("[f30a2b70] playback ready 2000ms after request (2500ms after start), first finished pieces [0]",
            trace.ready())
        assertNull(trace.ready())
    }
}
