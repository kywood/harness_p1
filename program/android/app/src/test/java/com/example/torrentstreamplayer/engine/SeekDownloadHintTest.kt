package com.example.torrentstreamplayer.engine
import org.junit.Assert.*
import org.junit.Test

class SeekDownloadHintTest {
    @Test fun cursorImmediatelyTargetsCorrespondingFileRegion() {
        assertEquals(500L, SeekDownloadHint.offset(50_000, 100_000, 1000))
        assertEquals(250L, SeekDownloadHint.offset(25_000, 100_000, 1000))
    }
    @Test fun hintStaysInsideFileAtBothEnds() {
        assertEquals(0L, SeekDownloadHint.offset(-1, 100, 1000))
        assertEquals(999L, SeekDownloadHint.offset(200, 100, 1000))
        assertEquals(0L, SeekDownloadHint.offset(100, 100, 1))
    }
    @Test fun unknownDurationOrSizeDoesNotGuessAnOffset() {
        assertNull(SeekDownloadHint.offset(50, 0, 1000))
        assertNull(SeekDownloadHint.offset(50, 100, 0))
        assertNull(SeekDownloadHint.offset(50, -100, 1000))
    }
    @Test fun largeFilesDoNotOverflowMultiplication() {
        val offset = SeekDownloadHint.offset(500_000_000, 1_000_000_000, 10_000_000_000)
        assertEquals(5_000_000_000L, offset)
    }
}
