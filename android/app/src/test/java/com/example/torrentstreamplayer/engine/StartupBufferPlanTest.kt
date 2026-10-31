package com.example.torrentstreamplayer.engine
import org.junit.Assert.*
import org.junit.Test

class StartupBufferPlanTest {
    private val mb = 1024L * 1024
    @Test fun coldMp4RequiresHeadWithoutBlindlyWaitingForTail() {
        val required = StartupBufferPlan.required(100 * mb, 0, mb, "mp4")
        assertEquals(setOf(0, 1), required)
        assertFalse(StartupBufferPlan.ready(required) { it == 0 })
        assertTrue(StartupBufferPlan.ready(required) { it < 2 })
    }
    @Test fun manyDownloadedPiecesDoNotHideMissingInitialPiece() {
        val required = StartupBufferPlan.required(100 * mb, 0, mb, "mp4")
        assertFalse(StartupBufferPlan.ready(required) { it != 1 })
        assertTrue(StartupBufferPlan.ready(required) { it != 99 })
    }
    @Test fun firstAttemptBecomesReadyAsDownloadProgressesWithoutRestart() {
        val required = StartupBufferPlan.required(100 * mb, 0, mb, "mp4")
        val downloaded = (8..99).toMutableSet()
        assertFalse(StartupBufferPlan.ready(required, downloaded::contains))
        downloaded.addAll(0..7)
        assertTrue(StartupBufferPlan.ready(required, downloaded::contains))
    }
    @Test fun movingDownloadWindowDoesNotDemoteStartupHeaderOrIndex() {
        val required = StartupBufferPlan.required(100 * mb, 0, mb, "mp4")
        assertEquals(7, StartupBufferPlan.priority(0, 4..7, required, 1))
        assertEquals(1, StartupBufferPlan.priority(99, 4..7, required, 1))
        assertEquals(1, StartupBufferPlan.priority(50, 4..7, required, 1))
        assertEquals(1, StartupBufferPlan.priority(0, 4..7, emptySet(), 1))
    }
    @Test fun otherContainersOnlyRequireHeadAndShortFilesStayInBounds() {
        assertEquals(setOf(0, 1), StartupBufferPlan.required(100 * mb, 0, mb, "mkv"))
        assertEquals(setOf(0, 1), StartupBufferPlan.required(mb, mb / 2, mb, "MP4"))
        assertEquals(setOf(0), StartupBufferPlan.required(1, 0, mb, "mp4"))
    }
    @Test fun emptyOrInvalidPlanCannotStartPlayback() {
        assertTrue(StartupBufferPlan.required(0, 0, mb, "mp4").isEmpty())
        assertTrue(StartupBufferPlan.required(mb, -1, mb, "mp4").isEmpty())
        assertTrue(StartupBufferPlan.required(mb, 0, 0, "mp4").isEmpty())
        assertFalse(StartupBufferPlan.ready(emptySet()) { true })
    }
}
