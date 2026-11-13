package com.example.torrentstreamplayer.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistSummaryTest {
    private fun entry(key: String, phase: DownloadPhase) = DownloadEntry(key, phase)

    @Test fun countsQueuedAndWifiWaitingSeparately() {
        val jobs = listOf(entry("a", DownloadPhase.RUNNING), entry("b", DownloadPhase.QUEUED),
            entry("c", DownloadPhase.WAITING_FOR_WIFI), entry("d", DownloadPhase.WAITING_FOR_WIFI),
            entry("e", DownloadPhase.COMPLETE), entry("f", DownloadPhase.FAILED))
        assertEquals(PlaylistSummary(downloading = 1, queued = 1, waitingForWifi = 2), PlaylistSummary.of(jobs, 1))
    }

    @Test fun zeroPartsAreLeftOutButDownloadingStays() {
        val summary = PlaylistSummary.of(listOf(entry("a", DownloadPhase.WAITING_FOR_WIFI)), downloading = 0)
        assertEquals(0, summary.downloading)
        assertFalse(summary.showsQueued)
        assertTrue(summary.showsWifiWaiting)
        val none = PlaylistSummary.of(emptyList(), downloading = 2)
        assertFalse(none.showsQueued)
        assertFalse(none.showsWifiWaiting)
    }

    @Test fun wifiWaitingJobsKeepTheListLive() {
        assertTrue(PlaylistSummary.of(listOf(entry("a", DownloadPhase.WAITING_FOR_WIFI)), 0).hasLiveJobs)
        assertFalse(PlaylistSummary.of(listOf(entry("a", DownloadPhase.COMPLETE)), 0).hasLiveJobs)
    }

    @Test fun negativeDownloadingCountIsClamped() {
        assertEquals(0, PlaylistSummary.of(emptyList(), downloading = -1).downloading)
    }
}
