package com.example.torrentstreamplayer.engine

internal object SeekDownloadHint {
    fun offset(timeMs: Long, durationMs: Long, fileSize: Long): Long? {
        if (durationMs <= 0 || fileSize <= 0) return null
        return (timeMs.coerceIn(0, durationMs).toDouble() / durationMs * fileSize)
            .toLong().coerceIn(0, fileSize - 1)
    }
}
