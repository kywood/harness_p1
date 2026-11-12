package com.example.torrentstreamplayer.ui

import androidx.annotation.StringRes
import com.example.torrentstreamplayer.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** A one-time message after "받아 두기", shown as a snackbar. [action] is the snackbar button text, if any. */
internal enum class IncomingLinkNotice(@StringRes val message: Int, @StringRes val action: Int?) {
    /** Added to the playlist; the snackbar offers "재생 목록". */
    SAVED(R.string.open_link_saved, R.string.action_open_playlist),
    SAVE_FAILED(R.string.error_download_start, null),
}

/**
 * Text the main input should take. [serial] grows with every fill, so the same text can be filled twice.
 * An empty [text] clears the input.
 */
internal data class MainInputFill(val text: String, val serial: Long)

/**
 * Leads the "link from another app → confirm → play or save" flow, so [MainViewModel] only wires it up.
 * The participants come in as functions; they never call each other directly. Call from the main thread.
 *
 * @param findInPlaylist the playlist entry with this magnet key, or null. Reads disk, so it switches threads itself;
 * a failure counts as "not in the playlist" (unknown membership must not block the sheet).
 * @param isPlaybackActive true while something plays or prepares; checked again when the user presses play.
 * @param startPlayback starts playback of a link through the ViewModel's play-from-main path, which also asks the
 * "Wi-Fi 전용" mobile data question.
 * @param saveLink adds a link as a download without playing it; throws when the link cannot be resolved.
 */
internal class IncomingLinkCoordinator(
    private val scope: CoroutineScope,
    private val findInPlaylist: suspend (key: String) -> PlaylistMatch?,
    private val isPlaybackActive: () -> Boolean,
    private val startPlayback: (String) -> Unit,
    private val saveLink: suspend (String) -> Unit,
) {
    private val mutableState = MutableStateFlow<IncomingLinkState?>(null)
    /** What the link sheet shows, or null when no sheet is open. A newer link replaces the sheet's content. */
    val state: StateFlow<IncomingLinkState?> = mutableState.asStateFlow()
    private val mutableNotice = MutableStateFlow<IncomingLinkNotice?>(null)
    /** The message to show once; the screen calls [noticeShown] after showing it. */
    val notice: StateFlow<IncomingLinkNotice?> = mutableNotice.asStateFlow()
    private val mutableInput = MutableStateFlow<MainInputFill?>(null)
    /** Fills the main input with a received link, and clears it after "받아 두기". Never set for a rejected link. */
    val mainInput: StateFlow<MainInputFill?> = mutableInput.asStateFlow()
    private var version = 0L

    fun receive(result: IncomingLinkResult) {
        val current = ++version
        when (result) {
            is IncomingLinkResult.Invalid -> mutableState.value = IncomingLinkState.Rejected(result.reason)
            is IncomingLinkResult.Valid -> {
                val link = result.link
                fillInput(link.value)
                // Shown at once but busy, so an older sheet's buttons cannot act while the playlist is read.
                mutableState.value = IncomingLinkPolicy.state(link, null, isPlaybackActive()).copy(busy = true)
                scope.launch {
                    val match = link.magnetKey?.let { key ->
                        try { findInPlaylist(key) }
                        catch (cancelled: CancellationException) { throw cancelled }
                        catch (_: Exception) { null }
                    }
                    if (current == version) mutableState.value = IncomingLinkPolicy.state(link, match, isPlaybackActive())
                }
            }
        }
    }

    /**
     * "바로 재생" or "이어서 재생" (playback resumes from the saved position by itself). Ignored (returns false) unless a
     * valid link waits, the sheet is not busy and nothing plays now: the allowed actions are computed again here
     * because playback may have started or ended while the sheet was open.
     */
    fun play(): Boolean {
        val pending = mutableState.value as? IncomingLinkState.Confirm ?: return false
        if (pending.busy || IncomingLinkAction.PLAY_NOW !in IncomingLinkPolicy.allowed(isPlaybackActive())) return false
        close()
        startPlayback(pending.link.value)
        return true
    }

    /**
     * "받아 두기". The sheet is busy meanwhile, so a second press does nothing. Afterwards the sheet closes and
     * [notice] tells the result; on success the main input is cleared so the saved link is not played by mistake.
     * If a newer link replaced the sheet meanwhile, that sheet stays.
     */
    suspend fun save() {
        val pending = mutableState.value as? IncomingLinkState.Confirm ?: return
        if (pending.busy || IncomingLinkAction.SAVE !in pending.allowed) return
        val saving = pending.copy(busy = true)
        mutableState.value = saving
        val saved = try {
            saveLink(pending.link.value)
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            false
        }
        mutableNotice.value = if (saved) IncomingLinkNotice.SAVED else IncomingLinkNotice.SAVE_FAILED
        if (mutableState.value !== saving) return
        if (saved) fillInput("")
        close()
    }

    /** "취소", "확인" on the error sheet, or dismissing the sheet. The main input keeps the link. */
    fun dismiss() = close()

    fun noticeShown() {
        mutableNotice.value = null
    }

    private fun fillInput(text: String) {
        mutableInput.value = MainInputFill(text, (mutableInput.value?.serial ?: 0) + 1)
    }

    private fun close() {
        version++
        mutableState.value = null
    }
}
