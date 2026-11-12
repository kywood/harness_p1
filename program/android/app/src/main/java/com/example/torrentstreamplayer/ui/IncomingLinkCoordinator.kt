package com.example.torrentstreamplayer.ui

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Leads the "link from another app → confirm → play or save" flow, so [MainViewModel] only wires it up.
 * The participants come in as functions; they never call each other directly. Call from the main thread.
 *
 * @param playlistKeys magnet keys in the playlist; reads disk, so it switches threads itself.
 * @param isPlaybackActive true while something plays or prepares; checked again when the user presses play.
 * @param startPlayback starts playback of a link (the ViewModel's play-from-main path and its Wi-Fi only check).
 * @param saveLink adds a link as a download without playing it; throws when the link cannot be resolved.
 */
internal class IncomingLinkCoordinator(
    private val scope: CoroutineScope,
    private val playlistKeys: suspend () -> Set<String>,
    private val isPlaybackActive: () -> Boolean,
    private val startPlayback: (String) -> Unit,
    private val saveLink: suspend (String) -> Unit,
) {
    private val mutableState = MutableStateFlow<IncomingLinkState?>(null)
    /** What the link sheet shows, or null when no sheet is open. A newer link replaces the sheet's content. */
    val state: StateFlow<IncomingLinkState?> = mutableState.asStateFlow()
    private var version = 0L

    fun receive(result: IncomingLinkResult) {
        val current = ++version
        when (result) {
            is IncomingLinkResult.Invalid -> mutableState.value = IncomingLinkState.Rejected(result.reason)
            is IncomingLinkResult.Valid -> scope.launch {
                val keys = try { playlistKeys() }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { emptySet() } // Unknown membership must not block the sheet.
                if (current == version) {
                    mutableState.value = IncomingLinkPolicy.state(result.link, keys, isPlaybackActive())
                }
            }
        }
    }

    /**
     * "바로 재생". Ignored (returns false) unless a valid link waits and nothing plays now: the allowed actions are
     * computed again here because playback may have started or ended while the sheet was open.
     */
    fun play(): Boolean {
        val pending = mutableState.value as? IncomingLinkState.Confirm ?: return false
        if (IncomingLinkAction.PLAY_NOW !in IncomingLinkPolicy.allowed(isPlaybackActive())) return false
        close()
        startPlayback(pending.link.value)
        return true
    }

    /**
     * "받아 두기". On success the sheet closes. Resolving a `.torrent` address can fail: the exception is rethrown
     * and the sheet stays, so it can show the reason. A magnet the download list itself rejects is reported through
     * the download list's error, not here.
     */
    suspend fun save() {
        val pending = mutableState.value as? IncomingLinkState.Confirm ?: return
        saveLink(pending.link.value)
        if (mutableState.value === pending) close()
    }

    /** "취소", "확인" on the error sheet, or dismissing the sheet. */
    fun dismiss() = close()

    private fun close() {
        version++
        mutableState.value = null
    }
}
