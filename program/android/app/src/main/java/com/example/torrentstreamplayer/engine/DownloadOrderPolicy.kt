package com.example.torrentstreamplayer.engine

/** The order a task downloads its selected video in. */
internal enum class DownloadOrder(val sequential: Boolean) {
    /**
     * A download-only task fetches from the file start, the way playback reads it, so a partly downloaded video
     * plays at once. Rarest-first would only help the swarm, and this app never uploads.
     */
    FILE_START(sequential = true),

    /** Playback owns the task: deadline windows follow the read position, which native sequential would fight. */
    PLAYBACK_WINDOW(sequential = false),

    /** A download-only task the user set to [DownloadOrderPreference.FASTEST]: libtorrent picks any offered piece. */
    RAREST_FIRST(sequential = false),
}

internal object DownloadOrderPolicy {
    fun order(playbackFocused: Boolean, preference: DownloadOrderPreference = DownloadOrderPreference.DEFAULT) = when {
        playbackFocused -> DownloadOrder.PLAYBACK_WINDOW
        else -> if (preference == DownloadOrderPreference.FASTEST) DownloadOrder.RAREST_FIRST else DownloadOrder.FILE_START
    }

    /** First and last two pieces of the video: the header and, for many containers, the index. */
    fun edgePieces(first: Int, last: Int): Set<Int> =
        if (first < 0 || last < first) emptySet()
        else setOf(first, first + 1, last - 1, last).filterTo(mutableSetOf()) { it in first..last }
}
