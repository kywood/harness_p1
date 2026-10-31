package com.example.torrentstreamplayer.engine
import org.junit.Assert.*
import org.junit.Test

class PlaybackDownloadPolicyTest {
    private val mb = 1024L * 1024
    @Test fun missingPlaybackBufferConcentratesDownloadsWithoutBlockingBackground() {
        val priority = PlaybackDownloadPolicy.backgroundPriority(20 * mb, 100 * mb, 0, mb) { it != 22 }
        assertEquals(1, priority)
        assertEquals(7, SequentialPiecePlan.priority(22, 20..23, priority))
        assertTrue(SequentialPiecePlan.priority(50, 20..23, priority) > 0)
    }
    @Test fun eightMegabytesAheadRestoresNormalBackgroundDownload() {
        assertEquals(4, PlaybackDownloadPolicy.backgroundPriority(20 * mb, 100 * mb, 0, mb) { it in 20..27 })
    }
    @Test fun seekingToMissingBufferConcentratesDownloadsAgain() {
        val downloaded: (Int) -> Boolean = { it in 20..27 }
        assertEquals(4, PlaybackDownloadPolicy.backgroundPriority(20 * mb, 100 * mb, 0, mb, downloaded))
        assertEquals(1, PlaybackDownloadPolicy.backgroundPriority(60 * mb, 100 * mb, 0, mb, downloaded))
    }
    @Test fun shortTailAndUnalignedFileUseOnlyRequiredPieces() {
        assertEquals(4, PlaybackDownloadPolicy.backgroundPriority(9 * mb, 10 * mb, mb / 2, mb) { it in 9..10 })
        assertEquals(1, PlaybackDownloadPolicy.backgroundPriority(9 * mb, 10 * mb, mb / 2, mb) { it == 9 })
    }
    @Test fun unknownInputKeepsLowBackgroundRatherThanDisablingIt() {
        assertEquals(1, PlaybackDownloadPolicy.backgroundPriority(0, 0, 0, mb) { false })
        assertEquals(1, PlaybackDownloadPolicy.backgroundPriority(0, mb, 0, 0) { false })
    }
}
