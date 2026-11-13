package com.example.torrentstreamplayer.ui

import com.example.torrentstreamplayer.R
import com.example.torrentstreamplayer.ui.components.StatusTone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistItemStatusTest {
    @Test fun waitingForWifiWinsOverRunningAndQueued() {
        assertEquals(PlaylistItemStatus.WAITING_FOR_WIFI,
            PlaylistItemStatus.of(active = true, queued = false, failed = false, complete = false, waitingForWifi = true))
        assertEquals(PlaylistItemStatus.WAITING_FOR_WIFI,
            PlaylistItemStatus.of(active = false, queued = true, failed = true, complete = false, waitingForWifi = true))
    }

    @Test fun waitingForWifiHidesSpeedAndShowsWifiIcon() {
        assertFalse(PlaylistItemStatus.WAITING_FOR_WIFI.showsSpeed)
        assertEquals(R.drawable.ic_wifi, PlaylistItemStatus.WAITING_FOR_WIFI.icon)
        assertEquals(StatusTone.WAITING_WIFI, PlaylistItemStatus.WAITING_FOR_WIFI.tone)
        PlaylistItemStatus.entries.filter { it != PlaylistItemStatus.WAITING_FOR_WIFI }.forEach {
            assertTrue(it.showsSpeed)
            assertNull(it.icon)
        }
    }

    @Test fun queuedWinsOverEverythingElse() {
        assertEquals(PlaylistItemStatus.QUEUED, PlaylistItemStatus.of(active = false, queued = true, failed = true, complete = false))
    }

    @Test fun runningIncompleteDownloadIsDownloading() {
        assertEquals(PlaylistItemStatus.DOWNLOADING, PlaylistItemStatus.of(active = true, queued = false, failed = true, complete = false))
    }

    @Test fun completeFileIsCompletedEvenWhileTheJobWindsDown() {
        assertEquals(PlaylistItemStatus.COMPLETED, PlaylistItemStatus.of(active = true, queued = false, failed = false, complete = true))
    }

    @Test fun idleFailureAndIdlePartialAreDistinct() {
        assertEquals(PlaylistItemStatus.FAILED, PlaylistItemStatus.of(active = false, queued = false, failed = true, complete = false))
        assertEquals(PlaylistItemStatus.INCOMPLETE, PlaylistItemStatus.of(active = false, queued = false, failed = false, complete = false))
    }

    /** The playlist card and the link sheet share this one ([PlaylistItemStatus.of] with a task). */
    @Test fun aTaskGivesTheSameChipForCardAndSheet() {
        assertEquals(PlaylistItemStatus.WAITING_FOR_WIFI,
            PlaylistItemStatus.of(DownloadEntry("k", DownloadPhase.WAITING_FOR_WIFI), playing = false, complete = false))
        assertEquals(PlaylistItemStatus.QUEUED,
            PlaylistItemStatus.of(DownloadEntry("k", DownloadPhase.QUEUED), playing = false, complete = false))
        assertEquals(PlaylistItemStatus.DOWNLOADING,
            PlaylistItemStatus.of(DownloadEntry("k", DownloadPhase.RUNNING), playing = false, complete = false))
        assertEquals("the playing video counts as downloading without a task",
            PlaylistItemStatus.DOWNLOADING, PlaylistItemStatus.of(null, playing = true, complete = false))
        assertEquals(PlaylistItemStatus.FAILED,
            PlaylistItemStatus.of(DownloadEntry("k", DownloadPhase.FAILED, error = DownloadFailure.StartFailed),
                playing = false, complete = false))
        assertEquals(PlaylistItemStatus.COMPLETED, PlaylistItemStatus.of(null, playing = false, complete = true))
        assertEquals(PlaylistItemStatus.INCOMPLETE, PlaylistItemStatus.of(null, playing = false, complete = false))
    }
}
