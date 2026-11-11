package com.example.torrentstreamplayer.engine

import com.example.torrentstreamplayer.meta.EngineSpec

internal data class SequentialPieceRequest(val piece: Int, val deadlineMs: Int, val priority: Int)

/**
 * How many pieces ahead of the read position are urgent, and how far apart their deadlines are.
 * [of] sizes it from the stream's bitrate so the urgent range covers [EngineSpec.URGENT_BUFFER_SEC]
 * of playback whatever the piece size is.
 */
internal data class PlaybackBuffer(val pieces: Int, val msPerPiece: Int) {
    init { require(pieces >= 1 && msPerPiece >= 1) }

    companion object {
        /** The fixed window used before buffering was time-based; kept as the minimum. */
        val MINIMUM = PlaybackBuffer(EngineSpec.MIN_URGENT_PIECES, 750)

        fun of(pieceLength: Long, bytesPerSecond: Long): PlaybackBuffer {
            if (pieceLength <= 0) return MINIMUM
            val rate = bytesPerSecond.coerceAtLeast(EngineSpec.MIN_BYTES_PER_SECOND)
            val urgentBytes = minOf(rate * EngineSpec.URGENT_BUFFER_SEC, EngineSpec.MAX_URGENT_BYTES)
            val pieces = ((urgentBytes + pieceLength - 1) / pieceLength)
                .coerceIn(EngineSpec.MIN_URGENT_PIECES.toLong(), EngineSpec.MAX_URGENT_PIECES.toLong()).toInt()
            val msPerPiece = (pieceLength * 1000 / rate).coerceIn(1, Int.MAX_VALUE.toLong()).toInt()
            return PlaybackBuffer(pieces, msPerPiece)
        }

        /** Average bytes per second of a file, or null when the duration is too short to trust. */
        fun bytesPerSecond(fileSize: Long, durationMs: Long): Long? =
            if (fileSize <= 0 || durationMs < EngineSpec.MIN_DURATION_FOR_BITRATE_MS) null
            else fileSize * 1000 / durationMs
    }
}

internal object SequentialPiecePlan {
    fun priority(piece: Int, window: IntRange, background: Int = 1): Int = if (piece in window) 7 else background
    fun nextMissing(first: Int, last: Int, havePiece: (Int) -> Boolean): Int? =
        if (first < 0 || last < first) null else (first..last).firstOrNull { !havePiece(it) }
    fun window(first: Int, last: Int, buffer: PlaybackBuffer = PlaybackBuffer.MINIMUM): IntRange =
        if (first < 0 || last < first) IntRange.EMPTY else first..(first + minOf(buffer.pieces - 1, last - first))
    /** Missing pieces of the urgent window, each due when playback is expected to reach it. */
    fun requests(first: Int, last: Int, buffer: PlaybackBuffer = PlaybackBuffer.MINIMUM,
        havePiece: (Int) -> Boolean): List<SequentialPieceRequest> =
        window(first, last, buffer).filterNot(havePiece).mapIndexed { index, piece ->
            SequentialPieceRequest(piece, (index.toLong() * buffer.msPerPiece).coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                (7 - index).coerceAtLeast(4))
        }
}
