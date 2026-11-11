package com.example.torrentstreamplayer.engine

import com.example.torrentstreamplayer.meta.EngineSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackBufferTest {
    private val mib = 1024L * 1024

    @Test fun urgentRangeCoversThirtySecondsOfPlaybackForCommonPieceSizes() {
        // 8 Mbps (1 MiB/s) for 30 s is 30 MiB.
        assertEquals(15, PlaybackBuffer.of(pieceLength = 2 * mib, bytesPerSecond = mib).pieces)
        assertEquals(8, PlaybackBuffer.of(pieceLength = 4 * mib, bytesPerSecond = mib).pieces)
    }

    @Test fun smallPiecesAreCappedSoLibtorrentIsNotFlooded() {
        val buffer = PlaybackBuffer.of(pieceLength = 256 * 1024, bytesPerSecond = mib)
        assertEquals(EngineSpec.MAX_URGENT_PIECES, buffer.pieces)
    }

    @Test fun highBitrateIsCappedByBytesAndHugePiecesKeepTheMinimum() {
        // 40 Mbps for 30 s would be 150 MiB; the urgent range stops at 64 MiB.
        assertEquals(16, PlaybackBuffer.of(pieceLength = 4 * mib, bytesPerSecond = 5 * mib).pieces)
        assertEquals(EngineSpec.MIN_URGENT_PIECES, PlaybackBuffer.of(pieceLength = 32 * mib, bytesPerSecond = mib).pieces)
    }

    @Test fun deadlinesFollowExpectedPlaybackTime() {
        val buffer = PlaybackBuffer.of(pieceLength = 2 * mib, bytesPerSecond = mib)
        assertEquals(2_000, buffer.msPerPiece)
        val plan = SequentialPiecePlan.requests(100, 999, buffer) { false }
        assertEquals(listOf(0, 2_000, 4_000), plan.take(3).map { it.deadlineMs })
        assertEquals((100 until 115).toList(), plan.map { it.piece })
    }

    @Test fun bitrateIsEstimatedOnlyFromATrustworthyDuration() {
        assertEquals(mib, PlaybackBuffer.bytesPerSecond(fileSize = 600 * mib, durationMs = 600_000))
        assertNull(PlaybackBuffer.bytesPerSecond(fileSize = 600 * mib, durationMs = 1))
        assertNull(PlaybackBuffer.bytesPerSecond(fileSize = 0, durationMs = 600_000))
    }

    @Test fun unknownOrInvalidPieceLengthFallsBackToTheMinimumWindow() {
        assertEquals(PlaybackBuffer.MINIMUM, PlaybackBuffer.of(pieceLength = 0, bytesPerSecond = mib))
        // A zero rate is raised to the 1 Mbps floor: 30 s = 3.75 MiB -> 4 pieces of 1 MiB, 8 s apart.
        assertEquals(PlaybackBuffer(pieces = 4, msPerPiece = 8_000), PlaybackBuffer.of(pieceLength = mib, bytesPerSecond = 0))
    }

    @Test fun startupBufferStaysTwoMegabytesAtTheDefaultBitrate() {
        assertEquals(setOf(0, 1), StartupBufferPlan.required(size = 100 * mib, offset = 0, pieceSize = mib, extension = "mkv"))
    }
}
