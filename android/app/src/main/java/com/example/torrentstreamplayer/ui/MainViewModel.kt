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
    val torrentEngine: TorrentEngine get() = engine.value
    internal val isTorrentEngineInitialized: Boolean get() = engine.isInitialized()
    private val history by lazy { MagnetHistory(TorrentCache(
        java.io.File(application.noBackupFilesDir, "magnet_history"), "0".repeat(64),
        TorrentCacheCipher::encrypt, TorrentCacheCipher::decrypt)) }
    private var inputJob: Job? = null
    fun savedMagnets(): List<SavedMagnet> = history.list()
    fun deleteSavedMagnet(magnet: String) = history.delete(magnet)

    private val _uiState = MutableStateFlow<StreamUiState>(StreamUiState.Idle)
    val uiState: StateFlow<StreamUiState> = _uiState.asStateFlow()

    private var autoPlayTriggered = false
    private var requestVersion = 0L

    fun startStreaming(magnetUrl: String) {
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
        torrentEngine.startStreaming(
            magnetUrl = trimmed,
            onMetadataReceived = { fileName, fileSize ->
                viewModelScope.launch {
                    if (request != requestVersion) return@launch
                    _uiState.value = StreamUiState.Buffering(
                        fileName = fileName,
                        totalSize = fileSize,
                        progress = 0f,
                        downloadSpeedBytes = 0L
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
                        val isReady = torrentEngine.isPlaybackReady()
                        if (isReady && !autoPlayTriggered) {
                            autoPlayTriggered = true
                            _uiState.value = StreamUiState.Playing(
                                fileName = currentState.fileName
                            )
                        }
                    }
                }
            },
            onError = { errorMsg ->
                viewModelScope.launch {
                    if (request != requestVersion) return@launch
                    _uiState.value = StreamUiState.Error(errorMsg)
                }
            }
        )
    }

    fun stopStream() {
        inputJob?.cancel()
        requestVersion++
        if (engine.isInitialized()) engine.value.stop()
        _uiState.value = StreamUiState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        if (engine.isInitialized()) engine.value.stop()
    }
}
