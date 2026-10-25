package com.example.torrentstreamplayer.engine

internal object PlaybackDownloadPolicy {
    fun backgroundPriority(position: Long, size: Long, offset: Long, pieceSize: Long,
        havePiece: (Int) -> Boolean): Int {
        if (size <= 0 || pieceSize <= 0 || position !in 0 until size) return 1
        val first = (offset + position) / pieceSize
        val end = offset + position + minOf(8L * 1024 * 1024, size - position) - 1
        return if ((first..(end / pieceSize)).all { havePiece(it.toInt()) }) 4 else 1
    }
}
