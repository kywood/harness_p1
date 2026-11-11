package com.example.torrentstreamplayer.ui
import org.junit.Assert.*
import org.junit.Test
class DownloadSelectionTest {
    @Test fun runningAndQueuedSelectedTasksSwitchActionToStop() {
        val jobs = mapOf("a" to DownloadEntry("a", DownloadPhase.RUNNING), "b" to DownloadEntry("b"),
            "done" to DownloadEntry("done", DownloadPhase.COMPLETE), "failed" to DownloadEntry("failed", DownloadPhase.FAILED))
        val stop = DownloadSelection.stoppable(setOf("a", "b", "done", "failed"), jobs)
        assertEquals(setOf("a", "b"), stop); assertEquals("중지", DownloadSelection.label(stop))
    }
    @Test fun otherDownloadsDoNotChangeSelectedIdleAction() {
        val jobs = mapOf("other" to DownloadEntry("other", DownloadPhase.RUNNING))
        val stop = DownloadSelection.stoppable(setOf("idle"), jobs)
        assertTrue(stop.isEmpty()); assertEquals("다운로드", DownloadSelection.label(stop))
    }
    @Test fun emptyOrCompletedSelectionCannotStopUnrelatedJobs() {
        val jobs = mapOf("a" to DownloadEntry("a", DownloadPhase.COMPLETE))
        assertTrue(DownloadSelection.stoppable(emptySet(), jobs).isEmpty())
        assertTrue(DownloadSelection.stoppable(setOf("a"), jobs).isEmpty())
        assertEquals(setOf("legacy"), DownloadSelection.stoppable(setOf("legacy"), emptyMap(), "legacy"))
    }
}
