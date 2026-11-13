package com.example.torrentstreamplayer.ui

internal object VideoPlaybackPolicy {
    /**
     * Text encoding VLC uses for subtitle files that are not UTF-8. Korean `.smi`/`.srt` files are mostly CP949
     * (EUC-KR); UTF-8 files are still read as UTF-8 because VLC checks for UTF-8 first (`subsdec-autodetect-utf8`).
     */
    const val SUBTITLE_FALLBACK_ENCODING = "CP949"

    /** VLC options; verbose native logging (-vvv) floods logcat during playback, so only debug builds enable it. */
    fun options(debug: Boolean): ArrayList<String> =
        arrayListOf("--drop-late-frames", "--skip-frames", "--file-caching=2000",
            "--subsdec-encoding=$SUBTITLE_FALLBACK_ENCODING").apply { if (debug) add("-vvv") }
    fun hardwareDecoding(hardware: String): Boolean =
        !hardware.contains("goldfish", ignoreCase = true) && !hardware.contains("ranchu", ignoreCase = true)
}
