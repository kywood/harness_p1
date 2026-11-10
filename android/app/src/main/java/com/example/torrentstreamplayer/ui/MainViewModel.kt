package com.example.torrentstreamplayer.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.torrentstreamplayer.engine.TorrentEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.withLock
import com.example.torrentstreamplayer.engine.MagnetHistory
import com.example.torrentstreamplayer.engine.TorrentCache
import com.example.torrentstreamplayer.engine.TorrentCacheCipher
import com.example.torrentstreamplayer.engine.SavedMagnet

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val engine = lazy { TorrentEngine(application.applicationContext) }
    private var sharedPlayback: TorrentEngine? = null
    val torrentEngine: TorrentEngine get() = sharedPlayback ?: engine.value
    internal val isTorrentEngineInitialized: Boolean get() = engine.isInitialized()
    private val history by lazy { MagnetHistory(TorrentCache(
        java.io.File(application.noBackupFilesDir, "magnet_history"), "0".repeat(64),
        TorrentCacheCipher::encrypt, TorrentCacheCipher::decrypt)) }
    private var inputJob: Job? = null
    private val positionWrites = kotlinx.coroutines.sync.Mutex()
    private var playingMagnet: String? = null
    var resumePositionMs: Long = 0; private set
    fun savedMagnets(): List<SavedMagnet> = history.list().map(torrentEngine::savedMagnet)
    fun deleteSavedMagnet(magnet: String) {
        val key = com.example.torrentstreamplayer.engine.MagnetIdentity.key(magnet)
        val protectedFiles = kotlinx.coroutines.runBlocking(Dispatchers.Main.immediate) {
            if (_activeKey.value == key) stopStream()
            downloads.stop(key)
            listOfNotNull(if (engine.isInitialized()) engine.value.selectedVideoFile else null) + downloads.protectedFiles()
        }
        if (engine.isInitialized()) engine.value.awaitStoppedWrites()
        downloads.awaitStoppedWrites()
        torrentEngine.deleteTorrentFiles(magnet, protectedFiles)
        history.delete(magnet)
    }
    internal val downloads = DownloadManager(viewModelScope, { DownloadController(viewModelScope, { TorrentEngine(application.applicationContext) },
        { withContext(Dispatchers.IO) { history.add(it) }; Unit },
        { magnet, name -> withContext(Dispatchers.IO) { history.updateTitle(magnet, name) }; Unit }) })
    fun deleteStoredFile(path: String): Boolean {
        check(downloads.tasks.value.values.none { it.phase == DownloadPhase.RUNNING || it.phase == DownloadPhase.QUEUED }) { "파일을 삭제하려면 먼저 재생 목록의 다운로드를 중지해 주세요." }
        return torrentEngine.deleteStoredFile(path)
    }

    private val _uiState = MutableStateFlow<StreamUiState>(StreamUiState.Idle)
    val uiState: StateFlow<StreamUiState> = _uiState.asStateFlow()

    private var continueDownloading = false
    private val _activeKey = MutableStateFlow<String?>(null)
    val activeKey = _activeKey.asStateFlow()
    private var requestVersion = 0L

    fun startStreaming(magnetUrl: String, downloadOnly: Boolean = false) {
        if (downloadOnly) {
            downloads.start(magnetUrl) { key -> key != _activeKey.value }
            return
        }
        stopStream()
        continueDownloading = false
        _activeKey.value = null
        inputJob?.cancel()
        val request = ++requestVersion
        val trimmed = magnetUrl.trim()
        if (trimmed.isEmpty()) {
            _uiState.value = StreamUiState.Error("Magnet link cannot be empty")
            return
        }

        _uiState.value = StreamUiState.FetchingMetadata(trimmed)
        inputJob = viewModelScope.launch {
            try {
                val resolved = withContext(Dispatchers.IO) { torrentEngine.resolveInput(trimmed) }
                if (request != requestVersion) return@launch
                if (!continueDownloading) _activeKey.value = com.example.torrentstreamplayer.engine.MagnetIdentity.key(resolved)
                val lastPosition = withContext(Dispatchers.IO) {
                    history.add(resolved)
                    history.list().firstOrNull { com.example.torrentstreamplayer.engine.MagnetIdentity.key(it.magnet) == com.example.torrentstreamplayer.engine.MagnetIdentity.key(resolved) }?.lastPositionMs ?: 0
                }
                if (request == requestVersion) { playingMagnet = resolved; resumePositionMs = lastPosition }

                if (request == requestVersion) startResolved(resolved, request)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                if (request == requestVersion)
                    _uiState.value = StreamUiState.Error(e.localizedMessage ?: "주소를 처리하지 못했습니다.")
            }
        }
    }

    private fun startResolved(magnet: String, request: Long) {
        val key = com.example.torrentstreamplayer.engine.MagnetIdentity.key(magnet)
        downloads.start(magnet) { true }
        if (continueDownloading) { _activeKey.value = null; _uiState.value = StreamUiState.Idle; return }
        val source = downloads.borrow(key) ?: error("다운로드 작업을 연결하지 못했습니다.")
        startSharedPlayback(magnet, request, source)
    }
    private fun startSharedPlayback(magnet: String, request: Long, source: TorrentEngine) {
        sharedPlayback = source
        _uiState.value = StreamUiState.FetchingMetadata(magnet, engineReady = true)
        inputJob = viewModelScope.launch {
            var focused = false
            while (request == requestVersion) {
                val task = _activeKey.value?.let { downloads.entry(it) }
                if (task == null || task.phase == DownloadPhase.FAILED) {
                    _uiState.value = StreamUiState.Error(task?.error ?: "다운로드가 중지되었습니다.")
                    break
                }
                if (source.selectedVideoFile != null) {
                    if (!focused) focused = source.requestPlayback()
                    _uiState.value = if (source.isPlaybackReady()) StreamUiState.Playing(source.selectedVideoFile!!.name)
                    else StreamUiState.Buffering(source.selectedVideoFile!!.name, source.selectedFileLength,
                        source.downloadProgress, source.downloadSpeedBytes)
                    if (_uiState.value is StreamUiState.Playing) {
                        withContext(Dispatchers.IO) { runCatching { history.markPlayed(magnet) } }
                        break
                    }
                }
                kotlinx.coroutines.delay(250)
            }
        }
    }
    fun stopDownload(key: String) {
        stopDownloads(setOf(key))
    }
    suspend fun queueDownload(input: String) {
        val resolved = withContext(Dispatchers.IO) { torrentEngine.resolveInput(input.trim()) }
        downloads.start(resolved) { true }
    }
    suspend fun resetPlaybackPosition(magnet: String) = positionWrites.withLock {
        withContext(Dispatchers.IO) { history.savePosition(magnet, 0, 0) }
    }
    fun continueInPlaylist() {
        continueDownloading = true
        if (sharedPlayback != null) stopStream()
        else { _activeKey.value = null; _uiState.value = StreamUiState.Idle }
    }
    fun playbackPositionWriter(): (Long, Long) -> Unit {
        val magnet = playingMagnet ?: return { _, _ -> }
        return { position, duration -> viewModelScope.launch { positionWrites.withLock {
            withContext(Dispatchers.IO) { runCatching { history.savePosition(magnet, position.coerceAtLeast(0), duration.coerceAtLeast(0)) }
                .onFailure { android.util.Log.w("PlaybackPosition", "Unable to save playback position", it) } }
        } }; Unit }
    }
    fun stopForMain() {
        continueDownloading = false
        val key = _activeKey.value
        stopStream()
        if (key != null) downloads.stop(key)
    }
    fun stopDownloads(keys: Set<String>) {
        if (_activeKey.value in keys && sharedPlayback != null) stopStream()
        downloads.stop(keys)
    }
    fun stopStream() {
        val shared = sharedPlayback
        sharedPlayback = null
        if (shared != null) shared.releasePlaybackPriority()
        _activeKey.value?.let { downloads.release(it) }
        _activeKey.value = null
        inputJob?.cancel()
        requestVersion++
        if (engine.isInitialized()) engine.value.stop()
        _uiState.value = StreamUiState.Idle
    }

    fun stopDownloads() {
        if (sharedPlayback != null) stopStream()
        downloads.stop()
    }
    override fun onCleared() {
        super.onCleared()
        downloads.stop()
        if (engine.isInitialized()) engine.value.stop()
    }
}
