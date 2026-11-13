package com.example.torrentstreamplayer.engine

import com.example.torrentstreamplayer.meta.EngineSpec

internal object StartupBufferPlan {
    /** Bytes from the file start that must be verified before playback starts. */
    const val HEAD_BYTES = EngineSpec.STARTUP_BUFFER_SEC * EngineSpec.DEFAULT_BYTES_PER_SECOND
    fun required(size: Long, offset: Long, pieceSize: Long, extension: String): Set<Int> {
        if (size <= 0 || offset < 0 || pieceSize <= 0) return emptySet()
        val first = (offset / pieceSize).toInt()
        val headEnd = ((offset + minOf(size, HEAD_BYTES) - 1) / pieceSize).toInt()
        val pieces = (first..headEnd).toMutableSet()
        return pieces
    }
    fun ready(required: Set<Int>, havePiece: (Int) -> Boolean): Boolean =
        required.isNotEmpty() && required.all(havePiece)
    fun priority(piece: Int, window: IntRange, required: Set<Int>, background: Int): Int =
        if (piece in required) 7 else SequentialPiecePlan.priority(piece, window, background)
}
