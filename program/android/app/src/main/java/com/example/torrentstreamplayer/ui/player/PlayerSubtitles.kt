package com.example.torrentstreamplayer.ui.player

import com.example.torrentstreamplayer.engine.SubtitleFile
import com.example.torrentstreamplayer.ui.SubtitleSession
import com.example.torrentstreamplayer.ui.SubtitleSheetState
import com.example.torrentstreamplayer.ui.SubtitleSnapshot
import com.example.torrentstreamplayer.ui.TrackName
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Runs a [SubtitleSession] for the player screen (coordinator between the player, the engine's subtitle files and
 * the sheet). Every session call runs on [player] — the engine command queue, which also orders the player's
 * release — and results come back to [scope] (the screen's main-thread scope).
 *
 * @param onLanguageChosen the viewer picked a subtitle with a language; saved as the preferred language.
 */
internal class PlayerSubtitles(
    private val session: SubtitleSession,
    private val scope: CoroutineScope,
    private val player: CoroutineDispatcher,
    private val onLanguageChosen: (String) -> Unit,
) {
    private val closed = AtomicBoolean(false)
    private val sheetOpen = AtomicBoolean(false)
    private val mutableSheet = MutableStateFlow(SubtitleSheetState())
    private val autoPicks = Channel<TrackName>(Channel.BUFFERED)
    private val failures = Channel<Unit>(Channel.CONFLATED)
    private val refreshes = Channel<Unit>(Channel.CONFLATED)
    private var recheck: Job? = null

    val sheet: StateFlow<SubtitleSheetState> = mutableSheet.asStateFlow()

    /** Automatic picks, for the notice chip. */
    val autoPicked: Flow<TrackName> = autoPicks.receiveAsFlow()

    /** A pick the player refused while the sheet is open, for the "다시 골라 주세요" message. */
    val selectFailed: Flow<Unit> = failures.receiveAsFlow()

    init {
        scope.launch { refreshes.receiveAsFlow().collect { run { session.refresh() } } }
    }

    /** VLC started playing. */
    fun start() = launchRun { session.start() }

    /** VLC added, removed or switched a track; requests are merged while one is running. */
    fun refresh() {
        refreshes.trySend(Unit)
    }

    fun updateFiles(ready: List<SubtitleFile>, pending: List<File>) = launchRun { session.updateFiles(ready, pending) }

    /** The sheet opened or closed. A failure still on its way when the sheet closes is dropped, not shown later. */
    fun sheetShown(open: Boolean) {
        sheetOpen.set(open)
        if (!open) while (failures.tryReceive().isSuccess) Unit
    }

    fun selectSubtitle(id: Int) = scope.launch {
        val result = onPlayer {
            val ok = session.selectSubtitle(id)
            Triple(ok, session.languageKeyOf(id), session.refresh())
        } ?: return@launch
        val (ok, languageKey, snapshot) = result
        apply(snapshot)
        if (!ok) reportFailure() else languageKey?.let(onLanguageChosen)
    }

    fun selectAudio(id: Int) = scope.launch {
        val result = onPlayer { session.selectAudio(id) to session.refresh() } ?: return@launch
        apply(result.second)
        if (!result.first) reportFailure()
    }

    /** Call before releasing the player; later calls do nothing. */
    fun close() {
        closed.set(true)
        refreshes.close()
    }

    private fun reportFailure() {
        if (sheetOpen.get()) failures.trySend(Unit)
    }

    private fun launchRun(step: () -> SubtitleSnapshot) {
        scope.launch { run(step) }
    }

    private suspend fun run(step: () -> SubtitleSnapshot) {
        onPlayer(step)?.let(::apply)
    }

    private suspend fun <T> onPlayer(block: () -> T): T? =
        withContext(player) { if (closed.get()) null else block() }

    private fun apply(snapshot: SubtitleSnapshot) {
        mutableSheet.value = snapshot.sheet
        snapshot.autoPicked?.let { autoPicks.trySend(it) }
        recheck?.cancel()
        recheck = snapshot.recheckAfterMs?.let { waitMs ->
            scope.launch {
                delay(waitMs)
                refresh()
            }
        }
    }
}
