package com.example.torrentstreamplayer.ui

import androidx.annotation.StringRes
import com.example.torrentstreamplayer.R

/** Whether an incoming link is already in the playlist. */
internal enum class PlaylistMembership {
    IN_PLAYLIST,
    NEW,

    /** A `.torrent` address: its magnet is known only after downloading the file. */
    UNKNOWN,
}

/** Choices the confirmation sheet may offer besides "취소", which is always there. */
internal enum class IncomingLinkAction { PLAY_NOW, SAVE }

/**
 * The playlist entry with the same magnet key as an incoming link.
 *
 * @property magnet the entry's own link (same key); the status chip looks up its download task with it.
 * @property title the saved name (the received video's), or blank.
 * @property positionMs where "이어서 재생" starts; 0 when there is nothing to resume.
 * @property complete the video is fully downloaded, so there is nothing to save.
 */
internal data class PlaylistMatch(val magnet: String, val title: String, val positionMs: Long, val complete: Boolean)

/** What the link sheet shows. The user always confirms; nothing starts on its own. */
internal sealed interface IncomingLinkState {
    /**
     * A valid link waiting for the user's choice among [allowed] (or cancel).
     *
     * @property match the playlist entry for this link, if [membership] is [PlaylistMembership.IN_PLAYLIST].
     * @property busy the playlist is still being read or "받아 두기" is running: every button but "취소" is disabled,
     * so a press cannot act on a link the sheet is about to replace.
     */
    data class Confirm(
        val link: IncomingLink,
        val membership: PlaylistMembership,
        val allowed: Set<IncomingLinkAction>,
        val match: PlaylistMatch? = null,
        val busy: Boolean = false,
    ) : IncomingLinkState

    /** A link that cannot be opened; the sheet shows [reason] and a single confirm button. */
    data class Rejected(val reason: IncomingLinkError) : IncomingLinkState
}

/** A button of the link sheet. Each registers its text. */
internal enum class LinkSheetButton(@StringRes val label: Int) {
    PLAY_NOW(R.string.action_stream_play),
    RESUME(R.string.action_resume_play),
    SAVE(R.string.action_save_for_later),
}

/** The sheet's buttons above "취소": [primary] is drawn as the primary button, [secondary] below it. */
internal data class LinkSheetButtons(val primary: LinkSheetButton?, val secondary: LinkSheetButton?, val enabled: Boolean)

/** Decisions for links handed over by other apps. Pure; the activity and coordinator pass the facts in. */
internal object IncomingLinkPolicy {
    /**
     * An intent is a new hand-over only on a fresh launch or `onNewIntent`. A recreated activity
     * (configuration change, process restore) or a launch from Recents re-delivers an intent already handled.
     */
    fun shouldHandle(recreated: Boolean, launchedFromHistory: Boolean): Boolean = !recreated && !launchedFromHistory

    fun membership(link: IncomingLink, match: PlaylistMatch?): PlaylistMembership = when {
        link.magnetKey == null -> PlaylistMembership.UNKNOWN
        match != null -> PlaylistMembership.IN_PLAYLIST
        else -> PlaylistMembership.NEW
    }

    /** While something plays or prepares, "바로 재생" would end it, so only saving is offered. */
    fun allowed(playbackActive: Boolean): Set<IncomingLinkAction> =
        if (playbackActive) setOf(IncomingLinkAction.SAVE) else IncomingLinkAction.entries.toSet()

    fun state(link: IncomingLink, match: PlaylistMatch?, playbackActive: Boolean) =
        IncomingLinkState.Confirm(link, membership(link, match), allowed(playbackActive), match)

    /**
     * The snackbar button for [notice]. While something plays or prepares there is none: "재생 목록" would leave the
     * player, but the sheet promised that playback goes on (`open-link-sheet.md` 2-3).
     */
    @StringRes
    fun noticeAction(notice: IncomingLinkNotice, playbackActive: Boolean): Int? =
        if (playbackActive) null else notice.action

    /**
     * The status chip of a playlist entry, as the playlist card shows it. [task] is the entry's download task;
     * [playing] is true when it is the video playing now. Waiting for Wi-Fi shows as queued.
     */
    fun status(match: PlaylistMatch, task: DownloadEntry?, playing: Boolean): PlaylistItemStatus {
        val queued = task?.phase == DownloadPhase.QUEUED || task?.phase == DownloadPhase.WAITING_FOR_WIFI
        val active = task?.phase == DownloadPhase.RUNNING || playing
        return PlaylistItemStatus.of(active, queued, task?.error != null, match.complete)
    }

    /**
     * Buttons for [state] (`open-link-sheet.md` 2-1, 2-3). "받아 두기" is hidden when the playlist entry is fully
     * downloaded or its task is alive ([download] is [DownloadPhase.stoppable]: running, queued, waiting for Wi-Fi).
     * Playing shows "이어서 재생" when a position is saved.
     */
    fun buttons(state: IncomingLinkState.Confirm, download: DownloadPhase?): LinkSheetButtons {
        val match = state.match
        val canSave = IncomingLinkAction.SAVE in state.allowed && match?.complete != true && download?.stoppable != true
        val save = if (canSave) LinkSheetButton.SAVE else null
        val play = when {
            IncomingLinkAction.PLAY_NOW !in state.allowed -> null
            (match?.positionMs ?: 0) > 0 -> LinkSheetButton.RESUME
            else -> LinkSheetButton.PLAY_NOW
        }
        return if (play != null) LinkSheetButtons(play, save, !state.busy) else LinkSheetButtons(save, null, !state.busy)
    }
}
