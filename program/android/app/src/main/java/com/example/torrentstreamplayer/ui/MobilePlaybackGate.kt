package com.example.torrentstreamplayer.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The mobile-data confirmation held for one playback. Any new playback or stop clears it ([clear]), so a stale
 * request can never be confirmed after something else started. Call from the main thread.
 */
internal class MobilePlaybackGate {
    private val pending = MutableStateFlow<MobilePlaybackRequest?>(null)

    /** The request the confirmation dialog shows, or null when no dialog is open. */
    val request: StateFlow<MobilePlaybackRequest?> = pending.asStateFlow()

    /** Holds [request] until the user answers. */
    fun ask(request: MobilePlaybackRequest) {
        pending.value = request
    }

    /** "이번만 재생": returns the held request once and closes the dialog; null if nothing is held any more. */
    fun accept(): MobilePlaybackRequest? = pending.value.also { pending.value = null }

    /** "취소", a new playback or a stop: forgets the held request. */
    fun clear() {
        pending.value = null
    }
}
