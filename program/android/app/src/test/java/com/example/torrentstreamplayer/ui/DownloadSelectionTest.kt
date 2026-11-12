package com.example.torrentstreamplayer.ui
import com.example.torrentstreamplayer.R
import org.junit.Assert.*
import org.junit.Test
class DownloadSelectionTest {
    @Test fun runningQueuedAndWaitingSelectedTasksSwitchActionToStop() {
        val jobs = mapOf("a" to DownloadEntry("a", DownloadPhase.RUNNING), "b" to DownloadEntry("b"),
            "wifi" to DownloadEntry("wifi", DownloadPhase.WAITING_FOR_WIFI),
            "done" to DownloadEntry("done", DownloadPhase.COMPLETE), "failed" to DownloadEntry("failed", DownloadPhase.FAILED))
        val stop = DownloadSelection.stoppable(setOf("a", "b", "wifi", "done", "failed"), jobs)
        assertEquals(setOf("a", "b", "wifi"), stop)
        assertEquals(R.string.action_stop, DownloadSelection.label(stop))
    }
    @Test fun otherDownloadsDoNotChangeSelectedIdleAction() {
        val jobs = mapOf("other" to DownloadEntry("other", DownloadPhase.RUNNING))
        val stop = DownloadSelection.stoppable(setOf("idle"), jobs)
        assertTrue(stop.isEmpty()); assertEquals(R.string.action_download, DownloadSelection.label(stop))
    }
    @Test fun emptyOrCompletedSelectionCannotStopUnrelatedJobs() {
        val jobs = mapOf("a" to DownloadEntry("a", DownloadPhase.COMPLETE))
        assertTrue(DownloadSelection.stoppable(emptySet(), jobs).isEmpty())
        assertTrue(DownloadSelection.stoppable(setOf("a"), jobs).isEmpty())
        assertEquals(setOf("legacy"), DownloadSelection.stoppable(setOf("legacy"), emptyMap(), "legacy"))
    }
}
