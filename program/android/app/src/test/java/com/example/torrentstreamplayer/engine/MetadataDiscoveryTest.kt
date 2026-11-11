package com.example.torrentstreamplayer.engine
import org.junit.Assert.*
import org.junit.Test
class MetadataDiscoveryTest {
    @Test fun retriesPeerlessColdStartupWithoutRestartingTorrent() {
        val policy = MetadataDiscovery()
        assertFalse(policy.shouldRetry(4999, 0))
        assertTrue(policy.shouldRetry(5000, 0))
        assertFalse(policy.shouldRetry(19999, 0))
        assertTrue(policy.shouldRetry(20000, 0))
        assertTrue(policy.shouldRetry(50000, 0))
    }
    @Test fun connectedPeersAreAllowedToFinishMetadataWithoutForcedAnnounces() {
        val policy = MetadataDiscovery()
        assertFalse(policy.shouldRetry(60000, 2))
        assertTrue(policy.shouldRetry(61000, 0))
        assertFalse(policy.shouldRetry(61250, 0))
    }
    @Test fun delayedPollingDoesNotBurstThroughMissedRetries() {
        val policy = MetadataDiscovery()
        assertTrue(policy.shouldRetry(90000, 0))
        assertFalse(policy.shouldRetry(90250, 0))
        assertFalse(policy.shouldRetry(104999, 0))
        assertTrue(policy.shouldRetry(105000, 0))
    }
}
