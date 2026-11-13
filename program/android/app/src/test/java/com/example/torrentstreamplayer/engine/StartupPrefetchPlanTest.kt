package com.example.torrentstreamplayer.engine

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

    @Test fun invalidGeometryFetchesNothing() {
        assertTrue(StartupPrefetchPlan.tailPieces(0, 0, mb, "mp4").isEmpty())
        assertTrue(StartupPrefetchPlan.tailPieces(100 * mb, -1, mb, "mp4").isEmpty())
        assertTrue(StartupPrefetchPlan.tailPieces(100 * mb, 0, 0, "mp4").isEmpty())
    }
}
