package com.example.torrentstreamplayer.meta

import java.util.Locale

/**
 * Subtitle file formats the engine downloads next to the selected video. Register a new format here;
 * subtitle detection looks it up, so callers need no changes.
 */
internal enum class SubtitleFormat(val extension: String) {
    SRT("srt"),
    ASS("ass"),
    SSA("ssa"),
    VTT("vtt"),
    SUB("sub"),
    ;

    companion object {
        private val byExtension = entries.associateBy { it.extension }

        /** The format of the file at [path] by its extension (any case), or null. */
        fun of(path: String): SubtitleFormat? =
            byExtension[path.substringAfterLast('.', "").lowercase(Locale.ROOT)]
    }
}
