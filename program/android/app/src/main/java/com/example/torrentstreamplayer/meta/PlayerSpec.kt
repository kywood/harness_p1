package com.example.torrentstreamplayer.meta

/** Player screen timings and layout ratios (`design/specs/subtitles.md` 3절). */
internal object PlayerSpec {
    /** How long a notice chip ("자막: 한국어(파일)", "모바일 데이터로 재생 중") stays before it fades out. */
    const val NOTICE_MS = 2_500L

    /** A notice chip is at most this share of the screen width; longer text ends with an ellipsis. */
    const val NOTICE_MAX_WIDTH_FRACTION = 0.7f

    /**
     * How long the player may take to list a subtitle file it accepted. VLC opens added files later, and a file it
     * cannot read never gets a track; after this the file shows as "받지 못함" and the next file is added.
     */
    const val SUBTITLE_TRACK_TIMEOUT_MS = 5_000L
}
