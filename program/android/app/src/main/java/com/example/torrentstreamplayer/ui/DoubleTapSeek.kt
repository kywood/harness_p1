package com.example.torrentstreamplayer.ui

internal object DoubleTapSeek {
    fun target(x: Float, width: Float, position: Long, duration: Long): Long? {
        if (!x.isFinite() || !width.isFinite() || width <= 0 || duration <= 0) return null
        val current = position.coerceIn(0, duration)
        return if (x < width / 2) (current - 10_000).coerceAtLeast(0)
        else current + minOf(10_000, duration - current)
    }
}
