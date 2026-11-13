package com.example.torrentstreamplayer.engine

import com.example.torrentstreamplayer.meta.VideoFormat

/**
 * Pieces at the end of the video fetched together with the startup buffer, sized by
 * [VideoFormat.tailPrefetchBytes]. Many containers keep their index there; fetching it in parallel with the
 * head saves a round trip for an MP4 `moov` at the end and keeps VLC from waiting when it reads Matroska `Cues`.
 * Unlike [StartupBufferPlan.required], playback does not wait for these pieces.
 */
internal object StartupPrefetchPlan {
    fun tailPieces(size: Long, offset: Long, pieceSize: Long, extension: String): Set<Int> {
        val tailBytes = VideoFormat.ofExtension(extension)?.tailPrefetchBytes ?: 0L
        if (size <= 0 || offset < 0 || pieceSize <= 0 || tailBytes <= 0) return emptySet()
        val last = ((offset + size - 1) / pieceSize).toInt()
        val first = ((offset + size - minOf(size, tailBytes)) / pieceSize).toInt()
        return (first..last).toSet()
    }
}
