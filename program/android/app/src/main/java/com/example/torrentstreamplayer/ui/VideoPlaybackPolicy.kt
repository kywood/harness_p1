package com.example.torrentstreamplayer.ui

internal object VideoPlaybackPolicy {
    /**
     * VLC options; verbose native logging (-vvv) floods logcat during playback, so only debug builds enable it.
     * No subtitle encoding is forced: Korean CP949 files are re-encoded to UTF-8 by content before the player gets
     * them (`SubtitleEncodingPolicy`), and other files keep VLC's default (PR #142 review).
     */
    fun options(debug: Boolean): ArrayList<String> =
        arrayListOf("--drop-late-frames", "--skip-frames", "--file-caching=2000").apply { if (debug) add("-vvv") }
    fun hardwareDecoding(hardware: String): Boolean =
        !hardware.contains("goldfish", ignoreCase = true) && !hardware.contains("ranchu", ignoreCase = true)
}
