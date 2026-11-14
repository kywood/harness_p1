package com.example.torrentstreamplayer.engine

/**
 * The order the user wants a download-only task to fetch its video in. Chosen app-wide in settings and, optionally,
 * per video in the playlist. Playback ignores it: while a video plays, pieces follow the read position.
 *
 * @param code the value saved on disk (settings and [MagnetHistory]); never reuse or renumber a code. 0 means "none".
 * @param downloadOnly the engine order a download-only task uses for this choice.
 */
enum class DownloadOrderPreference(val code: Int, internal val downloadOnly: DownloadOrder) {
    /** From the file start, so a partly downloaded video plays at once. The default. */
    FROM_START(1, DownloadOrder.FILE_START),

    /** Any piece a peer offers (libtorrent's rarest-first): the whole file finishes sooner on thin swarms. */
    FASTEST(2, DownloadOrder.RAREST_FIRST);

    companion object {
        /** Saved code for "no choice" (a video that follows the app setting). */
        const val NO_CODE = 0

        val DEFAULT = FROM_START

        /** The preference saved as [code], or null for [NO_CODE] and unknown (damaged or newer) codes. */
        fun fromCode(code: Int): DownloadOrderPreference? = entries.firstOrNull { it.code == code }

        /** The code to save for [preference]; null (follow the app setting) is [NO_CODE]. */
        fun codeOf(preference: DownloadOrderPreference?): Int = preference?.code ?: NO_CODE

        /** The order a task uses: the video's own choice when it has one, else the app setting. */
        fun effective(perVideo: DownloadOrderPreference?, appDefault: DownloadOrderPreference): DownloadOrderPreference =
            perVideo ?: appDefault
    }
}
