package com.example.torrentstreamplayer.engine

internal object StartupBufferPlan {
    fun required(size: Long, offset: Long, pieceSize: Long, extension: String): Set<Int> {
        if (size <= 0 || offset < 0 || pieceSize <= 0) return emptySet()
        val first = (offset / pieceSize).toInt()
        val headEnd = ((offset + minOf(size, 2L * 1024 * 1024) - 1) / pieceSize).toInt()
        val pieces = (first..headEnd).toMutableSet()
        return pieces
    }
    fun ready(required: Set<Int>, havePiece: (Int) -> Boolean): Boolean =
        required.isNotEmpty() && required.all(havePiece)
    fun priority(piece: Int, window: IntRange, required: Set<Int>, background: Int): Int =
        if (piece in required) 7 else SequentialPiecePlan.priority(piece, window, background)
}
