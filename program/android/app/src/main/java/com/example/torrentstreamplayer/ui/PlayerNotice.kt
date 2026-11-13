package com.example.torrentstreamplayer.ui

/**
 * A short notice on the player (`PlayerNoticeChip`). Notices show one at a time in arrival order
 * ([PlayerNoticeQueue]); they are never stacked.
 */
internal sealed interface PlayerNotice {
    /** "자막: 한국어(파일)": the subtitle turned on automatically. */
    data class SubtitleOn(val name: TrackName) : PlayerNotice

    /** "모바일 데이터로 재생 중": Wi-Fi was lost while Wi-Fi only is on, and playback goes on over mobile data. */
    data object MobileData : PlayerNotice
}

/** Notices waiting to be shown; [current] is on screen. Immutable: each change returns a new queue. */
internal data class PlayerNoticeQueue(private val notices: List<PlayerNotice> = emptyList()) {
    val current: PlayerNotice? get() = notices.firstOrNull()

    /** Adds [notice] after the waiting ones; a notice already waiting is not added twice. */
    fun add(notice: PlayerNotice): PlayerNoticeQueue = if (notice in notices) this else copy(notices = notices + notice)

    /** The current notice's time is over. */
    fun next(): PlayerNoticeQueue = copy(notices = notices.drop(1))
}
