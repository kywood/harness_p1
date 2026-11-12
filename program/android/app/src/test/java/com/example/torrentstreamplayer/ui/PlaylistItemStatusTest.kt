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
}
