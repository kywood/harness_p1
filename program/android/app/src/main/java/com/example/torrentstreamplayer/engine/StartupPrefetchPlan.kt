package com.example.torrentstreamplayer.engine

import com.example.torrentstreamplayer.meta.EngineSpec
import com.example.torrentstreamplayer.meta.VideoFormat

/** Where the container index sits, as far as the startup index probe has seen. */
internal enum class IndexLocation {
    /** Not known yet (head not read), or the format has no index the probe can find (Matroska `Cues`). */
    UNKNOWN,

    /** Found at the file start (a "faststart" MP4): the tail is not needed for playback to start. */
    FRONT,

    /** Past the media data, so at the end of the file. */
    BACK,
}

/**
 * Pieces at the end of the video fetched together with the startup buffer, sized by
 * [VideoFormat.tailPrefetchBytes]. Many containers keep their index there; fetching it in parallel with the
 * head saves a round trip for an MP4 `moov` at the end and keeps VLC from waiting when it reads Matroska `Cues`.
 * Unlike [StartupBufferPlan.required], playback does not wait for these pieces, and their deadline is later than the
 * startup buffer's so they do not slow the head. Once the head shows the index at the front, the tail is dropped.
 */
internal object StartupPrefetchPlan {
    fun tailPieces(size: Long, offset: Long, pieceSize: Long, extension: String): Set<Int> {
        val tailBytes = VideoFormat.ofExtension(extension)?.tailPrefetchBytes ?: 0L
        if (size <= 0 || offset < 0 || pieceSize <= 0 || tailBytes <= 0) return emptySet()
        val last = ((offset + size - 1) / pieceSize).toInt()
        val first = ((offset + size - minOf(size, tailBytes)) / pieceSize).toInt()
        return (first..last).toSet()
    }

    /**
     * Classifies the file offset of the box header or index the probe last needed or found ([probeOffset], null when
     * it has not run). The probe walks top-level boxes in order, so reaching past [headBytes] means the media data
     * came first and the index is at the back.
     */
    fun locate(probeOffset: Long?, found: Boolean, headBytes: Long): IndexLocation = when {
        probeOffset == null -> IndexLocation.UNKNOWN
        probeOffset >= headBytes -> IndexLocation.BACK
        found -> IndexLocation.FRONT
        else -> IndexLocation.UNKNOWN
    }

    /** Tail pieces to keep pinned for [location]: none once the index is known to be at the front. */
    fun pieces(tail: Set<Int>, location: IndexLocation): Set<Int> =
        if (location == IndexLocation.FRONT) emptySet() else tail

    /** Deadline for a newly pinned [piece]: tail-only pieces wait behind the startup buffer. */
    fun deadlineMs(piece: Int, prefetch: Set<Int>, startup: Set<Int>): Int =
        if (piece in prefetch && piece !in startup) EngineSpec.TAIL_PREFETCH_DEADLINE_MS
        else EngineSpec.PIN_DEADLINE_MS
}
