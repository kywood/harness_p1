package com.example.torrentstreamplayer.engine

import java.io.File

/** A subtitle file the engine finished downloading (hash-verified) for the selected video. */
internal data class SubtitleFile(
    val file: File,
    /** Language guessed from the file name, or null when the name has no marker. */
    val language: SubtitleLanguage?,
    /** True when the name starts like the video's name, so it is that video's subtitle. */
    val matchesVideoName: Boolean,
)

/**
 * Picks the subtitle to show automatically. Files named like the video win over others
 * (a season pack can hold other episodes' subtitles); among those, [SubtitleLanguage] order
 * (Korean, then English), then the first file.
 */
internal object SubtitleSelectionPolicy {
    fun preferred(files: List<SubtitleFile>): SubtitleFile? {
        val pool = files.filter { it.matchesVideoName }.ifEmpty { files }
        return SubtitleLanguage.entries
            .firstNotNullOfOrNull { language -> pool.firstOrNull { it.language == language } }
            ?: pool.firstOrNull()
    }
}
