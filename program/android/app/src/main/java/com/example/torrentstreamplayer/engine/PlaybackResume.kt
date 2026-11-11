package com.example.torrentstreamplayer.engine

internal object PlaybackResume {
    fun position(positionMs: Long, durationMs: Long): Long {
        require(positionMs >= 0 && durationMs >= 0)
        return if (durationMs > 0 && positionMs >= durationMs - 3000) 0 else positionMs
    }
}
