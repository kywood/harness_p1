package com.example.torrentstreamplayer.ui
import android.util.Log
import com.example.torrentstreamplayer.engine.EngineFailureLog
import com.example.torrentstreamplayer.engine.MagnetIdentity
import com.example.torrentstreamplayer.engine.TorrentEngine
import com.example.torrentstreamplayer.meta.TorrentFailure
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
internal data class DownloadTaskState(val key: String? = null, val speed: Long = 0,
    val error: String? = null, val title: String? = null, val complete: Boolean = false) {
    val activeCount: Int get() = if (key != null && !complete && error == null) 1 else 0
}
internal class DownloadController(private val scope: CoroutineScope,
    private val createEngine: () -> TorrentEngine,
    private val save: suspend (String) -> Unit,
    private val title: suspend (String, String) -> Unit,
    private val commands: EngineCommandQueue = EngineCommandQueue.shared,
    /** Text for a failed start, from registered strings only. Never show raw library messages. */
    private val describeFailure: (Throwable) -> String) {
    private val lazyEngine = lazy(createEngine)
    val engine get() = lazyEngine.value
    private val mutableState = MutableStateFlow(DownloadTaskState())
    val state = mutableState.asStateFlow()
    private var job: Job? = null
    private var generation = 0L
    fun start(input: String, canStart: (String) -> Boolean) {
        stop()
        val request = generation
        job = scope.launch {
            try {
                val resolved = withContext(Dispatchers.IO) { engine.resolveInput(input.trim()) }
                val key = MagnetIdentity.key(resolved)
                if (!canStart(key)) throw TorrentFailure.ALREADY_DOWNLOADING.blocked()
                if (request != generation) return@launch
                mutableState.value = DownloadTaskState(key = key)
                save(resolved)
                if (request != generation) return@launch
                // startStreaming takes the engine lock; run it on the engine queue, never on the main thread.
                withContext(commands.dispatcher) { engine.startStreaming(resolved, onMetadataReceived = { name, _ ->
                    scope.launch {
                        if (request != generation) return@launch
                        mutableState.value = mutableState.value.copy(title = name)
                        // Saving the title is best effort; a failed history write must not crash the app.
                        try { title(resolved, name) }
                        catch (cancelled: CancellationException) { throw cancelled }
                        catch (failure: Exception) {
                            Log.w("DownloadController", "Unable to save title: ${EngineFailureLog.describe(failure)}")
                        }
                    }
                }, onProgress = { progress, speed ->
                    scope.launch {
                        if (request == generation) mutableState.value = mutableState.value.copy(
                            speed = speed, complete = progress >= 100f)
                    }
                }, onError = { error ->
                    scope.launch {
                        if (request == generation) {
                            stopEngine()
                            mutableState.value = DownloadTaskState(error = error)
                        }
                    }
                }, prioritizePlayback = false) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                if (request == generation) {
                    stopEngine()
                    mutableState.value = DownloadTaskState(error = describeFailure(error))
                }
            }
        }
    }
    /** Call from a background thread: waits for queued engine stops, then for their disk writes. */
    fun awaitStoppedWrites() {
        if (!commands.awaitIdle()) throw TorrentFailure.STOP_PENDING.blocked()
        if (lazyEngine.isInitialized()) engine.awaitStoppedWrites()
    }
    private fun stopEngine() {
        if (lazyEngine.isInitialized()) engine.let { stopping -> commands.post { stopping.stop() } }
    }
    fun stop() {
        generation++
        job?.cancel()
        job = null
        stopEngine()
        mutableState.value = DownloadTaskState()
    }
}
