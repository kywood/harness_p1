package com.example.torrentstreamplayer.engine

internal data class SequentialPieceRequest(val piece: Int, val deadlineMs: Int, val priority: Int)
internal object SequentialPiecePlan {
    fun priority(piece: Int, window: IntRange, background: Int = 1): Int = if (piece in window) 7 else background
    fun nextMissing(first: Int, last: Int, havePiece: (Int) -> Boolean): Int? =
        if (first < 0 || last < first) null else (first..last).firstOrNull { !havePiece(it) }
    fun window(first: Int, last: Int): IntRange =
        if (first < 0 || last < first) IntRange.EMPTY else first..(first + minOf(3, last - first))
    fun requests(first: Int, last: Int, havePiece: (Int) -> Boolean): List<SequentialPieceRequest> =
        window(first, last).filterNot(havePiece).mapIndexed { index, piece ->
            SequentialPieceRequest(piece, index * 750, (7 - index).coerceAtLeast(4))
        }
}
