package com.example.torrentstreamplayer.engine

import com.example.torrentstreamplayer.meta.EngineSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StartupPrefetchPlanTest {
    private val mb = 1024L * 1024

    @Test fun mp4FetchesTheMoovSizedTail() {
        assertEquals((96..99).toSet(), StartupPrefetchPlan.tailPieces(100 * mb, 0, mb, "mp4"))
        assertEquals((96..99).toSet(), StartupPrefetchPlan.tailPieces(100 * mb, 0, mb, "MOV"))
    }

    @Test fun matroskaFetchesASmallerTailForCues() {
        assertEquals(setOf(99), StartupPrefetchPlan.tailPieces(100 * mb, 0, mb, "mkv"))
        assertEquals(setOf(99), StartupPrefetchPlan.tailPieces(100 * mb, 0, mb, "webm"))
    }

    @Test fun formatsWithoutATrailingIndexFetchNothingExtra() {
        assertTrue(StartupPrefetchPlan.tailPieces(100 * mb, 0, mb, "avi").isEmpty())
        assertTrue(StartupPrefetchPlan.tailPieces(100 * mb, 0, mb, "ts").isEmpty())
        assertTrue(StartupPrefetchPlan.tailPieces(100 * mb, 0, mb, "unknown").isEmpty())
    }

    @Test fun tailUsesTorrentPiecesWhenTheVideoDoesNotStartAtZero() {
        // The video occupies torrent bytes [10 MB + 7, 110 MB + 7); its last 4 MB start inside piece 106.
        assertEquals((106..110).toSet(), StartupPrefetchPlan.tailPieces(100 * mb, 10 * mb + 7, mb, "mp4"))
    }

    @Test fun tinyVideosNeverReachBeforeTheirFirstPiece() {
        assertEquals(setOf(0), StartupPrefetchPlan.tailPieces(mb / 2, 0, mb, "mp4"))
        assertEquals(setOf(3), StartupPrefetchPlan.tailPieces(1000, 3 * mb + 10, mb, "mkv"))
    }

    @Test fun indexLocationIsUnknownUntilTheProbeHasSeenEnough() {
        assertEquals(IndexLocation.UNKNOWN, StartupPrefetchPlan.locate(null, found = false, headBytes = 2 * mb))
        // Still walking small boxes (ftyp, free) inside the head.
        assertEquals(IndexLocation.UNKNOWN, StartupPrefetchPlan.locate(32, found = false, headBytes = 2 * mb))
    }

    @Test fun indexFoundInTheHeadIsAtTheFront() {
        assertEquals(IndexLocation.FRONT, StartupPrefetchPlan.locate(32, found = true, headBytes = 2 * mb))
    }

    @Test fun probeReachingPastTheHeadMeansTheIndexIsAtTheBack() {
        assertEquals(IndexLocation.BACK, StartupPrefetchPlan.locate(90 * mb, found = false, headBytes = 2 * mb))
        assertEquals(IndexLocation.BACK, StartupPrefetchPlan.locate(90 * mb, found = true, headBytes = 2 * mb))
    }

    @Test fun faststartFilesDropTheTailOthersKeepIt() {
        val tail = StartupPrefetchPlan.tailPieces(100 * mb, 0, mb, "mp4")
        assertEquals(emptySet<Int>(), StartupPrefetchPlan.pieces(tail, IndexLocation.FRONT))
        assertEquals(tail, StartupPrefetchPlan.pieces(tail, IndexLocation.UNKNOWN))
        assertEquals(tail, StartupPrefetchPlan.pieces(tail, IndexLocation.BACK))
    }

    @Test fun tailPiecesWaitBehindTheStartupBuffer() {
        val startup = setOf(0, 1)
        val prefetch = setOf(1, 98, 99)
        assertEquals(EngineSpec.TAIL_PREFETCH_DEADLINE_MS, StartupPrefetchPlan.deadlineMs(99, prefetch, startup))
        assertEquals(EngineSpec.PIN_DEADLINE_MS, StartupPrefetchPlan.deadlineMs(0, prefetch, startup))
        // A piece in both (tiny file) keeps the startup deadline.
        assertEquals(EngineSpec.PIN_DEADLINE_MS, StartupPrefetchPlan.deadlineMs(1, prefetch, startup))
        assertTrue(EngineSpec.TAIL_PREFETCH_DEADLINE_MS > EngineSpec.PIN_DEADLINE_MS)
    }

    @Test fun invalidGeometryFetchesNothing() {
        assertTrue(StartupPrefetchPlan.tailPieces(0, 0, mb, "mp4").isEmpty())
        assertTrue(StartupPrefetchPlan.tailPieces(100 * mb, -1, mb, "mp4").isEmpty())
        assertTrue(StartupPrefetchPlan.tailPieces(100 * mb, 0, 0, "mp4").isEmpty())
    }
}
