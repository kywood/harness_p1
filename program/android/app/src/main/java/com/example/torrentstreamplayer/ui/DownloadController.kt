package com.example.torrentstreamplayer.ui
import com.example.torrentstreamplayer.engine.MagnetIdentity
import com.example.torrentstreamplayer.engine.TorrentEngine
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
    private val commands: EngineCommandQueue = EngineCommandQueue.shared) {
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
                check(canStart(key)) { "재생 중인 영상은 이미 다운로드 중입니다." }
                if (request != generation) return@launch
                mutableState.value = DownloadTaskState(key = key)
                save(resolved)
                if (request != generation) return@launch
                // startStreaming takes the engine lock; run it on the engine queue, never on the main thread.
                withContext(commands.dispatcher) { engine.startStreaming(resolved, onMetadataReceived = { name, _ ->
                    scope.launch {
                        if (request != generation) return@launch
                        mutableState.value = mutableState.value.copy(title = name)
                        title(resolved, name)
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
                    mutableState.value = DownloadTaskState(error = error.localizedMessage ?: "다운로드를 시작하지 못했습니다.")
                }
            }
        }
    }
    /** Call from a background thread: waits for queued engine stops, then for their disk writes. */
    fun awaitStoppedWrites() {
        check(commands.awaitIdle()) { "다운로드 종료를 기다리는 중입니다. 잠시 후 다시 삭제해 주세요." }
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
