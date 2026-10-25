package com.example.torrentstreamplayer.engine

internal class SeekRequestTracker {
    @Volatile private var requested = 0L
    @Volatile private var served = 0L
    @Synchronized fun beginSeek() { requested++ }
    @Synchronized fun acceptRequest(): Long = requested.also { served = it }
    fun isCurrent(generation: Long): Boolean = generation == served
    fun isPriorityCurrent(generation: Long): Boolean = generation == served && generation == requested
}
