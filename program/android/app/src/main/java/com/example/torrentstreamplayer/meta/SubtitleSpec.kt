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
     * A folder is a season pack when another video in it has a different episode marker ([EPISODE_WORD]), or when it
     * holds [PACK_MIN_BIG_VIDEOS] other videos of at least this many percent of the selected video's size.
     * Smaller videos (samples, trailers) do not count; one large extra next to a movie does not make a pack.
     */
    const val PACK_VIDEO_MIN_PERCENT = 25L

    /**
     * Smallest other video, in percent of the selected video's size, whose episode marker counts for a season pack.
     * Lower than [PACK_VIDEO_MIN_PERCENT] so a 0.9 GB episode next to a 4 GB special still counts (#138), while a
     * small extra (`Featurettes.E01.mkv`, 50 MB next to a 2 GB movie) does not (PR #142 review).
     */
    const val PACK_EPISODE_MIN_PERCENT = 10L

    /** Other large videos (see [PACK_VIDEO_MIN_PERCENT]) that make a folder without episode markers a season pack. */
    const val PACK_MIN_BIG_VIDEOS = 2

    /** Folders searched below the video's folder: the subtitle folder and one episode folder inside it. */
    const val MAX_FOLDER_DEPTH = 2

    /**
     * Release words (resolution, source, codec) where a name comparison stops: `Show.S01E01.1080p.WEB.mkv` compares
     * as `Show S01E01`, so it matches `Show.S01E01.ko.srt`. Matched against one lowercase word.
     */
    val QUALITY_WORD = Regex(
        "\\d{3,4}p|4k|uhd|hdr\\d*|web|webrip|webdl|hdtv|bluray|bdrip|brrip|dvdrip|remux|x26[45]|h26[45]|hevc|avc|" +
            "aac\\d*|ac3|dts|ddp?\\d*|10bit|amzn|nf",
    )

    /** Episode marker word (lowercase): `s01e02`, `e02`, `ep02`, `1x02`. */
    val EPISODE_WORD = Regex("s\\d{1,2}e\\d{1,3}|ep?\\d{1,3}|\\d{1,2}x\\d{2,3}")
}
