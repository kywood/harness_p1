package com.example.torrentstreamplayer.engine
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
class StartupIndexStateTest {
    @get:Rule val folder = TemporaryFolder()
    private fun video(): File = folder.newFile("video.mp4").apply {
        val bytes = java.nio.ByteBuffer.allocate(24)
        for (kind in listOf("ftyp", "mdat", "moov")) bytes.putInt(8).put(kind.toByteArray())
        writeBytes(bytes.array())
    }
    @Test fun verifiedIndexIsReadyWithoutAnyBackgroundPolling() {
        val index = StartupIndexState(video(), 24, 0, 8, "mp4")
        assertFalse(index.ready)
        assertEquals(setOf(2), index.update { true })
        assertTrue(index.ready)
    }
    @Test fun pollingAfterTheIndexIsReadyIsFreeAndAddsNothing() {
        val index = StartupIndexState(video(), 24, 0, 8, "mp4")
        index.update { true }
        var checks = 0
        repeat(10) { assertEquals(emptySet<Int>(), index.update { checks++; true }) }
        assertEquals(0, checks)
    }
    @Test fun pollingWhileAPieceIsMissingKeepsAskingForTheSamePieces() {
        val index = StartupIndexState(video(), 24, 0, 8, "mp4")
        repeat(3) { assertEquals(setOf(2), index.update { it < 2 }) }
        assertFalse(index.ready)
    }
    @Test fun indexLocationFollowsHowFarTheProbeWalked() {
        val index = StartupIndexState(video(), 24, 0, 8, "mp4")
        assertEquals(IndexLocation.UNKNOWN, index.location(headBytes = 16))
        // The probe needs the mdat header at 8: still inside a 16-byte head, so not known yet.
        index.update { it == 0 }
        assertEquals(IndexLocation.UNKNOWN, index.location(headBytes = 16))
        index.update { true }
        // moov sits at 16, after the media data: with a 16-byte head that is the back, with a larger head the front.
        assertEquals(IndexLocation.BACK, index.location(headBytes = 16))
        assertEquals(IndexLocation.FRONT, index.location(headBytes = 1024))
    }
    @Test fun formatsWithoutAnMp4IndexNeverLocateIt() {
        val index = StartupIndexState(video(), 24, 0, 8, "mkv")
        assertEquals(emptySet<Int>(), index.update { true })
        assertEquals(IndexLocation.UNKNOWN, index.location(headBytes = 16))
    }
    @Test fun missingIndexRemainsBlockedUntilItsPieceIsVerified() {
        val index = StartupIndexState(video(), 24, 0, 8, "mp4")
        assertEquals(setOf(2), index.update { it < 2 })
        assertFalse(index.ready)
        index.update { true }
        assertTrue(index.ready)
    }
}
