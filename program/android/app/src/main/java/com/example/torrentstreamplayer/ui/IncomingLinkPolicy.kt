package com.example.torrentstreamplayer.ui

import androidx.annotation.StringRes
import com.example.torrentstreamplayer.R
import com.example.torrentstreamplayer.ui.navigation.AppScreen

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

/**
 * When a "받아 두기" notice is shown ([IncomingLinkPolicy.noticeTiming]).
 *
 * @property persistent the snackbar stays until its button is pressed.
 */
internal enum class NoticeTiming(val persistent: Boolean) {
    SHOW(persistent = false),

    /** Raised during playback; shown after leaving the player or preparing screen. */
    WAIT(persistent = true),
    DROP(persistent = false),
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
     * When to show [notice], decided once when it is raised. Nothing playing → at once. While something plays or
     * prepares the shared snackbar is not drawn ([AppSnackbarPolicy]): a failure must still reach the user, or they
     * would think the link was saved, so it waits for a screen with the snackbar; a success seen later would only
     * confuse, so it is dropped (also when it is collected after an earlier waiting failure).
     */
    fun noticeTiming(notice: IncomingLinkNotice, playbackActive: Boolean): NoticeTiming = when {
        !playbackActive -> NoticeTiming.SHOW
        notice == IncomingLinkNotice.SAVE_FAILED -> NoticeTiming.WAIT
        else -> NoticeTiming.DROP
    }

    /**
     * The snackbar button. Shown at once: the notice's own ("재생 목록"). A waiting failure: "확인", and it stays until
     * pressed ([NoticeTiming.persistent]): playback often ends with a full-screen ad, and a timed snackbar would run
     * out behind it. "재생 목록" is never offered for a notice raised during playback.
     */
    @StringRes
    fun noticeAction(notice: IncomingLinkNotice, timing: NoticeTiming): Int? = when (timing) {
        NoticeTiming.SHOW -> notice.action
        NoticeTiming.WAIT -> R.string.action_confirm
        NoticeTiming.DROP -> null
    }

    /**
     * A waiting notice may show once the screen change has settled (AppRoot dismissed the old screen's snackbar and
     * then published [settledScreen]) on a screen that draws the shared snackbar.
     */
    fun waitingNoticeMayShow(settledScreen: AppScreen?): Boolean =
        settledScreen != null && AppSnackbarPolicy.visible(settledScreen)

    /**
     * After a successful "받아 두기" while new downloads wait for Wi-Fi, the snackbar says so (`info_waiting_for_wifi`,
     * `settings-wifi.md` 4절) instead of "재생 목록에 추가했습니다.".
     */
    fun announcesWifiWait(notice: IncomingLinkNotice, waitsForWifi: Boolean): Boolean =
        notice == IncomingLinkNotice.SAVED && waitsForWifi

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
