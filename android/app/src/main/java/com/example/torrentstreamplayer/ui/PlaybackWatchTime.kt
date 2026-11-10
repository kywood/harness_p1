package com.example.torrentstreamplayer.ui

internal class PlaybackWatchTime {
    var watchedMs = 0L
        private set
    private var previousTime = 0L
    private var previousPosition = 0L
    private var previouslyActive = false
    fun sample(nowMs: Long, positionMs: Long, active: Boolean) {
        val elapsed = nowMs - previousTime
        val advanced = positionMs - previousPosition
        if (previouslyActive && active && elapsed in 1..1500 && advanced in 1..5000)
            watchedMs += elapsed
        previousTime = nowMs
        previousPosition = positionMs
        previouslyActive = active
    }
}
