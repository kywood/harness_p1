package com.example.torrentstreamplayer.engine
import org.junit.Assert.*
import org.junit.Test
class PlaybackResumeTest {
    @Test fun unfinishedVideoKeepsExactPosition() { assertEquals(45000, PlaybackResume.position(45000, 90000)) }
    @Test fun endedOrChangedShorterVideoStartsFromBeginning() {
        assertEquals(0, PlaybackResume.position(89900, 90000))
        assertEquals(0, PlaybackResume.position(95000, 90000))
    }
    @Test fun unknownDurationPreservesPositionAndResetClearsIt() {
        assertEquals(45000, PlaybackResume.position(45000, 0))
        assertEquals(0, PlaybackResume.position(0, 0))
        assertThrows(IllegalArgumentException::class.java) { PlaybackResume.position(-1, 100) }
    }
}
