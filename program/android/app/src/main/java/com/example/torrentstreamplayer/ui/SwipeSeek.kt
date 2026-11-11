package com.example.torrentstreamplayer.ui

internal object SwipeSeek {
    fun target(distance: Float, width: Float, position: Long, duration: Long): Long? {
        if (!distance.isFinite() || !width.isFinite() || width <= 0 || duration <= 0) return null
        val current = position.coerceIn(0, duration)
        val span = duration
        val delta = ((distance.toDouble() / width).coerceIn(-1.0, 1.0) * span).toLong()
        return if (delta >= 0) current + minOf(delta, duration - current) else (current + delta).coerceAtLeast(0)
    }
}
