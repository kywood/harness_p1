package com.example.torrentstreamplayer.ui

/** Whether an incoming link is already in the playlist. */
internal enum class PlaylistMembership {
    IN_PLAYLIST,
    NEW,

    /** A `.torrent` address: its magnet is known only after downloading the file. */
    UNKNOWN,
}

/** What the confirmation sheet offers first. The user always confirms; nothing starts on its own. */
internal enum class IncomingLinkAction { PLAY_NOW, SAVE }

/** A link waiting for the user's choice (play now / save / cancel). */
internal data class IncomingLinkState(
    val link: IncomingLink,
    val membership: PlaylistMembership,
    val recommended: IncomingLinkAction,
)

/** Decisions for links handed over by other apps. Pure; the activity and ViewModel pass the facts in. */
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

    /** While something plays or prepares, saving does not interrupt it, so the sheet recommends saving. */
    fun recommended(playbackActive: Boolean): IncomingLinkAction =
        if (playbackActive) IncomingLinkAction.SAVE else IncomingLinkAction.PLAY_NOW

    fun state(link: IncomingLink, playlistKeys: Set<String>, playbackActive: Boolean) =
        IncomingLinkState(link, membership(link, playlistKeys), recommended(playbackActive))
}
