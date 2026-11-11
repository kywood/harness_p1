package com.example.torrentstreamplayer.engine

internal object VerifiedDownload {
    fun bytes(size: Long, offset: Long, pieceSize: Long, havePiece: (Int) -> Boolean): Long {
        if (size <= 0 || offset < 0 || pieceSize <= 0) return 0
        val end = offset + size
        var downloaded = 0L
        for (piece in (offset / pieceSize)..((end - 1) / pieceSize)) {
            if (havePiece(piece.toInt()))
                downloaded += minOf(end, (piece + 1) * pieceSize) - maxOf(offset, piece * pieceSize)
        }
        return downloaded
    }
}
