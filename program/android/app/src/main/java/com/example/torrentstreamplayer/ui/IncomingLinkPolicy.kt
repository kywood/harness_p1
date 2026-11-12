package com.example.torrentstreamplayer.ui

/** Whether an incoming link is already in the playlist. */
internal enum class PlaylistMembership {
    IN_PLAYLIST,
    NEW,

    /** A `.torrent` address: its magnet is known only after downloading the file. */
    UNKNOWN,
}

/** Choices the confirmation sheet may offer besides "취소", which is always there. */
internal enum class IncomingLinkAction { PLAY_NOW, SAVE }

/** What the link sheet shows. The user always confirms; nothing starts on its own. */
internal sealed interface IncomingLinkState {
    /** A valid link waiting for the user's choice among [allowed] (or cancel). */
    data class Confirm(
        val link: IncomingLink,
        val membership: PlaylistMembership,
        val allowed: Set<IncomingLinkAction>,
    ) : IncomingLinkState

    /** A link that cannot be opened; the sheet shows [reason] and a single confirm button. */
    data class Rejected(val reason: IncomingLinkError) : IncomingLinkState
}

/** Decisions for links handed over by other apps. Pure; the activity and coordinator pass the facts in. */
internal object IncomingLinkPolicy {
    /**
     * An intent is a new hand-over only on a fresh launch or `onNewIntent`. A recreated activity
     * (configuration change, process restore) or a launch from Recents re-delivers an intent already handled.
     */
    fun shouldHandle(recreated: Boolean, launchedFromHistory: Boolean): Boolean = !recreated && !launchedFromHistory

    fun membership(link: IncomingLink, playlistKeys: Set<String>): PlaylistMembership {
        val key = link.magnetKey ?: return PlaylistMembership.UNKNOWN
        return if (key in playlistKeys) PlaylistMembership.IN_PLAYLIST else PlaylistMembership.NEW
    }

    /** While something plays or prepares, "바로 재생" would end it, so only saving is offered. */
    fun allowed(playbackActive: Boolean): Set<IncomingLinkAction> =
        if (playbackActive) setOf(IncomingLinkAction.SAVE) else IncomingLinkAction.entries.toSet()

    fun state(link: IncomingLink, playlistKeys: Set<String>, playbackActive: Boolean) =
        IncomingLinkState.Confirm(link, membership(link, playlistKeys), allowed(playbackActive))
}
