package com.example.torrentstreamplayer.meta

/** Limits and folder names for subtitle files downloaded with the selected video. */
internal object SubtitleSpec {
    /** Largest subtitle file the engine downloads (2 MiB). Larger files are not plain subtitles. */
    const val MAX_FILE_BYTES = 2L * 1024 * 1024

    /** Total subtitle bytes downloaded for one torrent (10 MiB). */
    const val MAX_TOTAL_BYTES = 10L * 1024 * 1024

    /** Deadline for missing subtitle pieces; shorter than the startup buffer's so subtitles arrive first. */
    const val PIECE_DEADLINE_MS = 500

    /**
     * Folder names (lowercase) directly under the video's folder that hold its subtitles, e.g. `Subs/`.
     * One more folder below it is searched too (`Subs/<episode>/`), see [MAX_FOLDER_DEPTH].
     */
    val FOLDER_NAMES: Set<String> = setOf("subs", "sub", "subtitles", "subtitle")

    /**
     * A folder is a season pack when it holds another video at least this many percent of the selected video's size.
     * Smaller videos (samples, trailers) do not count.
     */
    const val PACK_VIDEO_MIN_PERCENT = 25L

    /** Folders searched below the video's folder: the subtitle folder and one episode folder inside it. */
    const val MAX_FOLDER_DEPTH = 2
}
