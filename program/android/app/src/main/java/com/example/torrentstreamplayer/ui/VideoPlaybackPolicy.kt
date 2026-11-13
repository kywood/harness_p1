package com.example.torrentstreamplayer.ui

internal object VideoPlaybackPolicy {
    /**
     * Turns off VLC's own search for subtitle files next to the video (`sub-autodetect-file`, on by default). The app
     * chooses and adds subtitle files itself (`SubtitleSession`, `MediaPlayer.addSlave`); with VLC's search on, a
     * finished video opened as a file URI got the same subtitle twice, and the extra track broke the session's
     * "new track = the file just added" matching. Slaves added through `addSlave` do not depend on this option.
     */
    const val NO_SUBTITLE_AUTODETECT = "--no-sub-autodetect-file"

    /**
     * VLC options; verbose native logging (-vvv) floods logcat during playback, so only debug builds enable it.
     * No subtitle encoding is forced: Korean CP949 files are re-encoded to UTF-8 by content before the player gets
     * them (`SubtitleEncodingPolicy`), and other files keep VLC's default (PR #142 review).
     */
    fun options(debug: Boolean): ArrayList<String> =
        arrayListOf("--drop-late-frames", "--skip-frames", "--file-caching=2000", NO_SUBTITLE_AUTODETECT)
            .apply { if (debug) add("-vvv") }
    fun hardwareDecoding(hardware: String): Boolean =
        !hardware.contains("goldfish", ignoreCase = true) && !hardware.contains("ranchu", ignoreCase = true)
}
