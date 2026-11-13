package com.example.torrentstreamplayer.ui

internal object VideoPlaybackPolicy {
    /**
     * Text encoding VLC uses for non-UTF-8 subtitle files on Korean devices. Korean `.smi`/`.srt` files are mostly
     * CP949 (EUC-KR); UTF-8 files are still read as UTF-8 because VLC checks for UTF-8 first
     * (`subsdec-autodetect-utf8`). On other devices VLC keeps its default (CP1252), so Western files with curly quotes
     * are not broken (global release, PR #142 review).
     */
    const val KOREAN_SUBTITLE_ENCODING = "CP949"

    /** Device language (ISO 639-1, `Locale.getDefault().language`) that gets [KOREAN_SUBTITLE_ENCODING]. */
    private const val KOREAN_LANGUAGE = "ko"

    /**
     * VLC options; verbose native logging (-vvv) floods logcat during playback, so only debug builds enable it.
     * [deviceLanguage] chooses the subtitle fallback encoding.
     */
    fun options(debug: Boolean, deviceLanguage: String): ArrayList<String> =
        arrayListOf("--drop-late-frames", "--skip-frames", "--file-caching=2000").apply {
            if (deviceLanguage == KOREAN_LANGUAGE) add("--subsdec-encoding=$KOREAN_SUBTITLE_ENCODING")
            if (debug) add("-vvv")
        }
    fun hardwareDecoding(hardware: String): Boolean =
        !hardware.contains("goldfish", ignoreCase = true) && !hardware.contains("ranchu", ignoreCase = true)
}
