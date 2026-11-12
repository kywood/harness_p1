package com.example.torrentstreamplayer.engine

import java.io.File

/** A subtitle file the engine finished downloading (hash-verified) for the selected video. */
internal data class SubtitleFile(
    val file: File,
    /** Language guessed from the file name, or null when the name has no marker. */
    val language: SubtitleLanguage?,
    /** True when the name (or its episode folder's name) matches the video's name word for word. */
    val matchesVideoName: Boolean,
    /** True when the video is alone in its folder, so a subtitle with another name may stand in for it. */
    val fallbackAllowed: Boolean = false,
)

/**
 * Picks the subtitle to show automatically: files named like the video first; only when there are none, files that
 * may stand in for it ([SubtitleFile.fallbackAllowed], never in a season pack). Within that pool, [SubtitleLanguage]
 * order (Korean, then English), then the first file.
 */
internal object SubtitleSelectionPolicy {
    fun preferred(files: List<SubtitleFile>): SubtitleFile? {
        val pool = files.filter { it.matchesVideoName }.ifEmpty { files.filter { it.fallbackAllowed } }
        return SubtitleLanguage.entries
            .firstNotNullOfOrNull { language -> pool.firstOrNull { it.language == language } }
            ?: pool.firstOrNull()
    }
}
