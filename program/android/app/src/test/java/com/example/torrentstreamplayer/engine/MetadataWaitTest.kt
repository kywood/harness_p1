package com.example.torrentstreamplayer.engine

import org.junit.Assert.*
import org.junit.Test

class MetadataWaitTest {
    @Test fun firstConnectionCanReceiveMetadataAfterOldFortyFiveSecondLimit() {
        var time = 0L
        var polls = 0
        val result = MetadataWait.await(120000, { time }, { time += 10000 }) {
            polls++
            if (it >= 60000) "metadata" else null
        }
        assertEquals("metadata", result)
        assertEquals(7, polls)
        assertEquals(60000, time)
    }
    @Test fun peerlessTorrentStopsAtBoundedTimeout() {
        var time = 0L
        val result = MetadataWait.await<String>(1000, { time }, { time += 250 }) { null }
        assertNull(result)
        assertEquals(1000, time)
    }
    @Test fun immediateMetadataDoesNotWaitForResumeDataOrStartupDelay() {
        assertEquals("cached", MetadataWait.await(1000, { 0 }, { fail("Must not wait") }) { "cached" })
    }
    @Test fun cancellationDoesNotContinuePollingOrRetry() {
        Thread.currentThread().interrupt()
        try {
            assertThrows(InterruptedException::class.java) {
                MetadataWait.await<String>(1000, { 0 }, { fail("Must not sleep") }) { fail("Must not poll"); null }
            }
        } finally { Thread.interrupted() }
    }
}
