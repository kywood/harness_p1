package com.example.torrentstreamplayer.engine
import org.junit.Assert.*
import org.junit.Test

class SeekRequestTrackerTest {
    @Test fun seekingPreservesCachedReadButStopsItsDownloadPriority() {
        val tracker = SeekRequestTracker()
        val old = tracker.acceptRequest()
        tracker.beginSeek()
        assertTrue(tracker.isCurrent(old))
        assertFalse(tracker.isPriorityCurrent(old))
        val current = tracker.acceptRequest()
        assertFalse(tracker.isCurrent(old))
        assertTrue(tracker.isCurrent(current))
    }
    @Test fun parallelContainerRequestsInSameSeekRemainValid() {
        val tracker = SeekRequestTracker()
        tracker.beginSeek()
        val header = tracker.acceptRequest()
        val footer = tracker.acceptRequest()
        assertTrue(tracker.isCurrent(header))
        assertTrue(tracker.isCurrent(footer))
    }
    @Test fun quickRepeatedSeeksUseLatestGeneration() {
        val tracker = SeekRequestTracker()
        val first = tracker.acceptRequest()
        tracker.beginSeek()
        val second = tracker.acceptRequest()
        tracker.beginSeek()
        val third = tracker.acceptRequest()
        assertFalse(tracker.isCurrent(first))
        assertFalse(tracker.isCurrent(second))
        assertTrue(tracker.isCurrent(third))
    }
}
