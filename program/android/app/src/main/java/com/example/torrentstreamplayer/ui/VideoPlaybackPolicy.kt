package com.example.torrentstreamplayer.ui

internal object VideoPlaybackPolicy {
    /** VLC options; verbose native logging (-vvv) floods logcat during playback, so only debug builds enable it. */
    fun options(debug: Boolean): ArrayList<String> =
        arrayListOf("--drop-late-frames", "--skip-frames", "--file-caching=2000").apply { if (debug) add("-vvv") }
    fun hardwareDecoding(hardware: String): Boolean =
        !hardware.contains("goldfish", ignoreCase = true) && !hardware.contains("ranchu", ignoreCase = true)
}
