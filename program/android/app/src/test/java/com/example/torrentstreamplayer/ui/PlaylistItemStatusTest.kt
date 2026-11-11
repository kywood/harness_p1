package com.example.torrentstreamplayer.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaylistItemStatusTest {
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
