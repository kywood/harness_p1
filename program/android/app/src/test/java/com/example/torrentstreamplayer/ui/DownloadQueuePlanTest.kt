package com.example.torrentstreamplayer.ui
import org.junit.Assert.*
import org.junit.Test
class DownloadQueuePlanTest {
    @Test fun fillsOnlyAvailableSlotsInQueueOrder() {
        val entries = listOf(DownloadEntry("a", DownloadPhase.RUNNING), DownloadEntry("b"), DownloadEntry("c"), DownloadEntry("d"))
        assertEquals(listOf("b", "c"), DownloadQueuePlan.ready(entries, 3))
    }
    @Test fun fullConcurrencyLimitLeavesAdditionalTasksQueued() {
        val entries = (1..3).map { DownloadEntry("$it", DownloadPhase.RUNNING) } + DownloadEntry("waiting")
        assertTrue(DownloadQueuePlan.ready(entries, 3).isEmpty())
    }
    @Test fun completionFailureAndRemovalReleaseSlots() {
        val entries = listOf(DownloadEntry("done", DownloadPhase.COMPLETE), DownloadEntry("failed", DownloadPhase.FAILED),
            DownloadEntry("active", DownloadPhase.RUNNING), DownloadEntry("next"))
        assertEquals(listOf("next"), DownloadQueuePlan.ready(entries, 2))
    }
    @Test fun invalidLimitIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { DownloadQueuePlan.ready(emptyList(), 0) }
    }
}
