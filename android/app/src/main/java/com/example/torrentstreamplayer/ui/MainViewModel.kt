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
    fun savedMagnets(): List<SavedMagnet> = history.list().map(torrentEngine::savedMagnet)
    fun deleteSavedMagnet(magnet: String) {
        val key = com.example.torrentstreamplayer.engine.MagnetIdentity.key(magnet)
        val protectedFiles = kotlinx.coroutines.runBlocking(Dispatchers.Main.immediate) {
            if (_activeKey.value == key) stopStream()
            if (downloads.state.value.key == key) downloads.stop()
            listOfNotNull(if (engine.isInitialized()) engine.value.selectedVideoFile else null,
                if (downloads.state.value.key != null) downloads.engine.selectedVideoFile else null)
        }
        if (engine.isInitialized()) engine.value.awaitStoppedWrites()
        downloads.awaitStoppedWrites()
        torrentEngine.deleteTorrentFiles(magnet, protectedFiles)
        history.delete(magnet)
    }
    internal val downloads = DownloadController(viewModelScope, { TorrentEngine(application.applicationContext) },
        { withContext(Dispatchers.IO) { history.add(it) }; Unit },
        { magnet, name -> withContext(Dispatchers.IO) { history.updateTitle(magnet, name) }; Unit })
    fun deleteStoredFile(path: String): Boolean {
        check(downloads.state.value.key == null) { "파일을 삭제하려면 먼저 재생 목록의 다운로드를 중지해 주세요." }
        return torrentEngine.deleteStoredFile(path)
    }

    private val _uiState = MutableStateFlow<StreamUiState>(StreamUiState.Idle)
    val uiState: StateFlow<StreamUiState> = _uiState.asStateFlow()

    private var autoPlayTriggered = false
    private var autoPlayEnabled = true
    private val _activeKey = MutableStateFlow<String?>(null)
    val activeKey = _activeKey.asStateFlow()
    private var requestVersion = 0L

    fun startStreaming(magnetUrl: String, downloadOnly: Boolean = false) {
        if (downloadOnly) {
            downloads.start(magnetUrl) { key -> key != _activeKey.value }
            return
        }
        stopStream()
        autoPlayEnabled = !downloadOnly
        _activeKey.value = null
        inputJob?.cancel()
        val request = ++requestVersion
        val trimmed = magnetUrl.trim()
        if (trimmed.isEmpty()) {
            _uiState.value = StreamUiState.Error("Magnet link cannot be empty")
            return
        }

        autoPlayTriggered = false
        _uiState.value = StreamUiState.FetchingMetadata(trimmed)
        inputJob = viewModelScope.launch {
            try {
                val resolved = withContext(Dispatchers.IO) { torrentEngine.resolveInput(trimmed) }
                if (request != requestVersion) return@launch
                _activeKey.value = com.example.torrentstreamplayer.engine.MagnetIdentity.key(resolved)
                withContext(Dispatchers.IO) { history.add(resolved) }

                if (request == requestVersion) startResolved(resolved, request)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                if (request == requestVersion)
                    _uiState.value = StreamUiState.Error(e.localizedMessage ?: "주소를 처리하지 못했습니다.")
            }
        }
    }
    private fun startResolved(trimmed: String, request: Long) {
        if (_activeKey.value == downloads.state.value.key) {
            startSharedPlayback(request)
            return
        }
        torrentEngine.startStreaming(
            magnetUrl = trimmed,
            onMetadataReceived = { fileName, fileSize ->
                viewModelScope.launch {
                    if (request != requestVersion) return@launch
                    _uiState.value = StreamUiState.Buffering(
                        fileName = fileName,
                        totalSize = fileSize,
                        progress = 0f,
                        downloadSpeedBytes = 0L,
                        autoPlay = autoPlayEnabled
                    )
                    withContext(Dispatchers.IO) { runCatching { history.updateTitle(trimmed, fileName) } }
                }
            },
            onProgress = { progress, speed ->
                viewModelScope.launch {
                    if (request != requestVersion) return@launch
                    val currentState = _uiState.value
                    if (currentState is StreamUiState.Buffering) {
                        _uiState.value = currentState.copy(
                            progress = progress,
                            downloadSpeedBytes = speed
                        )
                        // Start only after the initial continuous video buffer is hash-verified.
                        val isReady = currentState.autoPlay && torrentEngine.isPlaybackReady()
                        if (isReady && !autoPlayTriggered) {
                            autoPlayTriggered = true
                            _uiState.value = StreamUiState.Playing(
                                fileName = currentState.fileName
                            )
                        }
                    }
                }
            },
            prioritizePlayback = autoPlayEnabled,
            onError = { errorMsg ->
                viewModelScope.launch {
                    if (request != requestVersion) return@launch
                    _uiState.value = StreamUiState.Error(errorMsg)
                    _activeKey.value = null
                }
            }
        )
    }

    private fun startSharedPlayback(request: Long) {
        sharedPlayback = downloads.engine
        inputJob = viewModelScope.launch {
            var focused = false
            while (request == requestVersion) {
                val task = downloads.state.value
                val source = downloads.engine
                if (source.selectedVideoFile != null) {
                    if (!focused) focused = source.requestPlayback()
                    _uiState.value = if (source.isPlaybackReady()) StreamUiState.Playing(source.selectedVideoFile!!.name)
                    else StreamUiState.Buffering(source.selectedVideoFile!!.name, source.selectedFileLength,
                        source.downloadProgress, source.downloadSpeedBytes)
                    if (_uiState.value is StreamUiState.Playing) break
                }
                if (task.key != _activeKey.value || task.error != null) {
                    _uiState.value = StreamUiState.Error(task.error ?: "다운로드가 중지되었습니다.")
                    break
                }
                kotlinx.coroutines.delay(250)
            }
        }
    }
    fun stopStream() {
        val shared = sharedPlayback
        sharedPlayback = null
        if (shared != null) shared.releasePlaybackPriority()
        _activeKey.value = null
        inputJob?.cancel()
        requestVersion++
        if (engine.isInitialized()) engine.value.stop()
        _uiState.value = StreamUiState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        downloads.stop()
        if (engine.isInitialized()) engine.value.stop()
    }
}
