package com.example.torrentstreamplayer.engine

/**
 * Debug-only record of how a playback start went: when playback was requested, which pieces of the selected
 * video finished in what order, and how long it took to become ready. The engine creates it only in debug
 * builds and logs the lines it returns. Lines carry the first [KEY_CHARS] characters of the magnet key and
 * file-relative piece numbers only: never a magnet, info-hash or file name.
 */
internal class PlaybackStartTrace(
    key: String,
    private val now: () -> Long = { System.nanoTime() / 1_000_000 },
    private val pieceLimit: Int = DEFAULT_PIECE_LIMIT,
) {
    private val tag = key.take(KEY_CHARS)
    private val startedMs = now()
    private var requestedMs = -1L
    private var known: BooleanArray? = null
    private val order = mutableListOf<Int>()
    private var readyLogged = false

    init { require(pieceLimit > 0) }

    @Synchronized fun playbackRequested(): String {
        requestedMs = now()
        return "[$tag] playback requested at ${requestedMs - startedMs}ms, pieces done before: ${known?.count { it } ?: 0}"
    }

    /**
     * Compares [range] with the last call and returns a line listing newly finished pieces (relative to the
     * file's first piece), or null when nothing new is worth logging. The first call only records what was
     * already on disk (resumed data), so it reports a count instead of an order.
     */
    @Synchronized fun observe(range: IntRange, havePiece: (Int) -> Boolean): String? {
        if (range.isEmpty()) return null
        // Past the limit nothing more is logged; skip rebuilding the piece array every second.
        if (known != null && order.size >= pieceLimit) return null
        val previous = known
        val current = BooleanArray(range.last - range.first + 1) { havePiece(range.first + it) }
        known = current
        if (previous == null || previous.size != current.size)
            return "[$tag] resumed with ${current.count { it }}/${current.size} pieces"
        val added = current.indices.filter { current[it] && !previous[it] }.take(pieceLimit - order.size)
        if (added.isEmpty()) return null
        order += added
        return "[$tag] +${now() - startedMs}ms finished pieces $added"
    }

    /** The line for the first ready check after a request; null afterwards or before playback was requested. */
    @Synchronized fun ready(): String? {
        if (readyLogged || requestedMs < 0) return null
        readyLogged = true
        val at = now()
        return "[$tag] playback ready ${at - requestedMs}ms after request (${at - startedMs}ms after start), " +
            "first finished pieces $order"
    }

    companion object {
        const val KEY_CHARS = 8
        const val DEFAULT_PIECE_LIMIT = 48
    }
}
