package com.example.torrentstreamplayer.engine

import org.junit.Assert.*
import org.junit.Test

class TorrentPlaybackTest {
    @Test fun invalidHttpRangesAreRejected() {
        listOf("bytes=100-", "bytes=20-10", "bytes=-0", "bytes=-", "bytes=0-1,3-4",
            "bytes=-2-10", "bytes=999999999999999999999-", "items=0-1").forEach {
            assertNull(it, TorrentPlayback.httpRange(it, 100))
        }
        assertNull(TorrentPlayback.httpRange(null, 0))
    }
    @Test fun validHttpRangesSupportVlcSeeksAndSuffixes() {
        assertEquals(0L..99L, TorrentPlayback.httpRange(null, 100))
        assertEquals(10L..99L, TorrentPlayback.httpRange("bytes=10-", 100))
        assertEquals(90L..99L, TorrentPlayback.httpRange("bytes=-10", 100))
        assertEquals(20L..99L, TorrentPlayback.httpRange("bytes=20-999", 100))
        assertEquals(0L..99L, TorrentPlayback.httpRange("bytes=-999", 100))
    }
    @Test fun startupDoesNotWaitForWholeMovie() {
        val checked = mutableListOf<Int>()
        assertTrue(TorrentPlayback.startupReady(100_000_000, 0, 262144) {
            checked.add(it); it < 4
        })
        assertEquals(listOf(0, 1, 2, 3), checked)
    }
    @Test fun missingMiddlePiecePreventsPlaybackEvenWithHeaderAndFooter() {
        assertFalse(TorrentPlayback.startupReady(1024, 3, 256) { it != 2 })
        assertTrue(TorrentPlayback.startupReady(1024, 3, 256) { true })
        assertFalse(TorrentPlayback.startupReady(0, 0, 256) { true })
    }
    @Test fun readStopsAtUnalignedTorrentPieceBoundary() {
        assertEquals(2, TorrentPlayback.readSize(0, 100, 1000, 6, 8))
        assertEquals(8, TorrentPlayback.readSize(2, 100, 1000, 6, 8))
        assertEquals(1, TorrentPlayback.readSize(999, 100, 1000, 6, 8))
        assertEquals(0, TorrentPlayback.readSize(1000, 100, 1000, 6, 8))
    }
    @Test fun onlyNonEmptyVideosAreCandidates() {
        assertFalse(TorrentPlayback.isVideo("large.zip", 9_000_000))
        assertFalse(TorrentPlayback.isVideo("empty.mp4", 0))
        assertTrue(TorrentPlayback.isVideo("movie.MKV", 100))
    }
}
