package com.example.torrentstreamplayer.meta

/** Engine timing and buffering values. Register new engine values here instead of writing literals at call sites. */
internal object EngineSpec {
    /** How long background teardown waits for libtorrent to deliver resume data. */
    const val RESUME_SAVE_WAIT_MS = 1_500L

    /** How long a caller waits for earlier teardown before restarting or deleting files. */
    const val TEARDOWN_AWAIT_MS = 10_000L

    /** Seconds of playback ahead of the read position that are downloaded at top priority. */
    const val URGENT_BUFFER_SEC = 30L

    /** Bounds of that urgent range, so tiny pieces do not flood libtorrent and huge bitrates do not hog it. */
    const val MIN_URGENT_PIECES = 4
    const val MAX_URGENT_PIECES = 64
    const val MAX_URGENT_BYTES = 64L * 1024 * 1024

    /** Assumed bitrate (about 8 Mbps) until the player reports the video length. */
    const val DEFAULT_BYTES_PER_SECOND = 1024L * 1024

    /** Lower bound for an estimated bitrate (about 1 Mbps). */
    const val MIN_BYTES_PER_SECOND = 128L * 1024

    /** Durations shorter than this are not used to estimate a bitrate. */
    const val MIN_DURATION_FOR_BITRATE_MS = 10_000L

    /** Seconds of video from the file start that must be verified before playback starts. */
    const val STARTUP_BUFFER_SEC = 2L

    /**
     * End of an MP4/MOV file fetched early: covers the `moov` index of many files.
     * A larger one is still found piece by piece by `StartupIndexState`.
     */
    const val MOOV_TAIL_PREFETCH_BYTES = 4L * 1024 * 1024

    /** End of a Matroska/WebM file fetched early for its `Cues` (seek index), which is usually far smaller. */
    const val CUES_TAIL_PREFETCH_BYTES = 1L * 1024 * 1024

    /** Deadline of a pinned startup or index piece. */
    const val PIN_DEADLINE_MS = 1_000

    /** Deadline of a tail prefetch piece: later than the startup buffer so the head is fetched first. */
    const val TAIL_PREFETCH_DEADLINE_MS = 3_000
}
