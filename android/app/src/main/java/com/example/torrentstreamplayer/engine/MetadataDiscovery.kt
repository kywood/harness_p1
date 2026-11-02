package com.example.torrentstreamplayer.engine

internal class MetadataDiscovery {
    private var nextAttempt = 5000L
    private var interval = 15000L
    fun shouldRetry(elapsed: Long, peers: Int): Boolean {
        if (peers > 0 || elapsed < nextAttempt) return false
        nextAttempt = elapsed + interval
        interval = (interval * 2).coerceAtMost(30000L)
        return true
    }
}
