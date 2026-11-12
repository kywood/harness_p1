package com.example.torrentstreamplayer.ui

import android.app.Application
import android.util.Log
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import com.example.torrentstreamplayer.data.AppSettings
import com.example.torrentstreamplayer.data.ConnectionType
import com.example.torrentstreamplayer.data.NetworkMonitor
import com.example.torrentstreamplayer.data.PlaylistRepository
import com.example.torrentstreamplayer.data.SettingsRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import com.example.torrentstreamplayer.engine.EngineFailureLog
import com.example.torrentstreamplayer.engine.MagnetIdentity
import com.example.torrentstreamplayer.engine.MetadataDetails
import com.example.torrentstreamplayer.engine.PieceMapSnapshot
import com.example.torrentstreamplayer.engine.SavedMagnet
import com.example.torrentstreamplayer.engine.StoredTorrentFile
import com.example.torrentstreamplayer.ui.navigation.AppNavigator
import com.example.torrentstreamplayer.ui.navigation.AppScreen
import com.example.torrentstreamplayer.ui.navigation.PlaybackOrigin

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val commands = EngineCommandQueue.shared
    private val engine = lazy { TorrentEngine(application.applicationContext) }
    /** 재생에 빌려 쓰는 다운로드 작업의 엔진. 준비 화면의 조각 맵·토렌트 정보도 여기서 읽는다. */
    private val playbackSource = MutableStateFlow<TorrentEngine?>(null)
    private var sharedPlayback: TorrentEngine?
        get() = playbackSource.value
        set(value) { playbackSource.value = value }
    val torrentEngine: TorrentEngine get() = sharedPlayback ?: engine.value
    internal val isTorrentEngineInitialized: Boolean get() = engine.isInitialized()
    private val playlist by lazy { PlaylistRepository.create(application) { torrentEngine.savedMagnet(it) } }
    private var inputJob: Job? = null
    private val positionWrites = kotlinx.coroutines.sync.Mutex()
    private val resume = PlaybackResumeState()
    /** 재생 화면을 열 때 이동할 위치. 재생 중 저장한 위치를 따라가므로 화면을 다시 만들어도 이어 본다. */
    val resumePositionMs: Long get() = resume.positionMs
    fun savedMagnets(): List<SavedMagnet> = playlist.savedMagnets()
    fun storedFiles(): List<StoredTorrentFile> = torrentEngine.storedFiles()
    /** Stops [magnet] on the main thread, then waits for its writes and deletes its files on [Dispatchers.IO]. */
    suspend fun deleteSavedMagnet(magnet: String) {
        val key = MagnetIdentity.key(magnet)
        val (protectedFiles, writers) = withContext(Dispatchers.Main.immediate) {
            if (_activeKey.value == key) stopStream()
            downloads.stop(key)
            (listOfNotNull(if (engine.isInitialized()) engine.value.selectedVideoFile else null) +
                downloads.protectedFiles()) to downloads.stoppedWriters()
        }
        withContext(Dispatchers.IO) {
            awaitStoppedWrites(writers)
            torrentEngine.deleteTorrentFiles(magnet, protectedFiles)
            playlist.delete(magnet)
        }
    }
    /** Background thread: waits for queued engine stops and every stopped engine's disk writes. */
    private fun awaitStoppedWrites(writers: List<DownloadController>) {
        check(commands.awaitIdle()) { "다운로드 종료를 기다리는 중입니다. 잠시 후 다시 삭제해 주세요." }
        if (engine.isInitialized()) engine.value.awaitStoppedWrites()
        downloads.awaitStoppedWrites(writers)
    }
    internal val downloads = DownloadManager(viewModelScope, { DownloadController(viewModelScope, { TorrentEngine(application.applicationContext) },
        { withContext(Dispatchers.IO) { playlist.add(it) }; Unit },
        { magnet, name -> withContext(Dispatchers.IO) { playlist.updateTitle(magnet, name) }; Unit },
        describeFailure = { InputFailureText.message(it, application) }) })
    /** Stopped downloads may still be writing their files; waits for them on [Dispatchers.IO] before deleting. */
    suspend fun deleteStoredFile(path: String): Boolean {
        val writers = withContext(Dispatchers.Main.immediate) {
            check(downloads.tasks.value.values.none { it.phase.stoppable }) { "파일을 삭제하려면 먼저 재생 목록의 다운로드를 중지해 주세요." }
            downloads.stoppedWriters()
        }
        return withContext(Dispatchers.IO) {
            awaitStoppedWrites(writers)
            torrentEngine.deleteStoredFile(path)
        }
    }

    private val _uiState = MutableStateFlow<StreamUiState>(StreamUiState.Idle)
    val uiState: StateFlow<StreamUiState> = _uiState.asStateFlow()
    private val navigator = AppNavigator()

    /** 지금 보여 줄 화면. 이동 의도와 [uiState]를 함께 반영한다. */
    val screen: StateFlow<AppScreen> = navigator.screen
    private fun setStream(state: StreamUiState) = synchronized(navigator) {
        _uiState.value = state
        navigator.onStreamChanged(state)
    }

    /** 준비 화면에 보일 조각 맵. 재생에 빌린 엔진이 없으면 비어 있다. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val pieceMap: StateFlow<PieceMapSnapshot> = playbackSource
        .flatMapLatest { it?.pieceMap ?: flowOf(PieceMapSnapshot.EMPTY) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(replayExpirationMillis = 0), PieceMapSnapshot.EMPTY)

    /** 준비 화면에 보일 토렌트 정보. 재생에 빌린 엔진이 없으면 기본값이다. */
    @OptIn(ExperimentalCoroutinesApi::class)
    internal val metadataDetails: StateFlow<MetadataDetails> = playbackSource
        .flatMapLatest { it?.metadataDetails ?: flowOf(MetadataDetails()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(replayExpirationMillis = 0), MetadataDetails())

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
            setStream(StreamUiState.Error(getApplication<Application>().getString(com.example.torrentstreamplayer.R.string.error_empty_link)))
            return
        }

        setStream(StreamUiState.FetchingMetadata(trimmed))
        inputJob = viewModelScope.launch {
            try {
                val resolved = withContext(Dispatchers.IO) { torrentEngine.resolveInput(trimmed) }
                if (request != requestVersion) return@launch
                if (!continueDownloading) _activeKey.value = com.example.torrentstreamplayer.engine.MagnetIdentity.key(resolved)
                val lastPosition = withContext(Dispatchers.IO) { playlist.addForPlayback(resolved) }
                if (request == requestVersion) resume.start(resolved, lastPosition)

                if (request == requestVersion) startResolved(resolved, request)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                if (request == requestVersion)
                    setStream(StreamUiState.Error(InputFailureText.message(e, getApplication())
                        ?: "주소를 처리하지 못했습니다."))
            }
        }
    }

    private fun startResolved(magnet: String, request: Long) {
        val key = com.example.torrentstreamplayer.engine.MagnetIdentity.key(magnet)
        if (request == mobileAllowedRequest) downloads.allowOnce(key)
        downloads.start(magnet) { true }
        if (continueDownloading) { _activeKey.value = null; setStream(StreamUiState.Idle); return }
        val source = downloads.borrow(key) ?: error("다운로드 작업을 연결하지 못했습니다.")
        startSharedPlayback(magnet, request, source)
    }
    private fun startSharedPlayback(magnet: String, request: Long, source: TorrentEngine) {
        sharedPlayback = source
        setStream(StreamUiState.FetchingMetadata(magnet, engineReady = true))
        inputJob = viewModelScope.launch {
            var focused = false
            while (request == requestVersion) {
                val task = _activeKey.value?.let { downloads.entry(it) }
                if (task == null || task.phase == DownloadPhase.FAILED) {
                    setStream(StreamUiState.Error(task?.error ?: "다운로드가 중지되었습니다."))
                    break
                }
                // requestPlayback/isPlaybackReady take the engine lock and query libtorrent synchronously.
                val next = withContext(commands.dispatcher) {
                    val file = source.selectedVideoFile ?: return@withContext null
                    if (!focused) focused = source.requestPlayback()
                    if (source.isPlaybackReady()) StreamUiState.Playing(file.name)
                    else StreamUiState.Buffering(file.name, source.selectedFileLength,
                        source.downloadProgress, source.downloadSpeedBytes)
                }
                if (request != requestVersion) break
                if (next != null) {
                    setStream(next)
                    if (next is StreamUiState.Playing) {
                        withContext(Dispatchers.IO) { runCatching { playlist.markPlayed(magnet) } }
                        break
                    }
                }
                kotlinx.coroutines.delay(250)
            }
        }
    }
    private val settingsRepository by lazy { SettingsRepository.create(application) }
    private val networkMonitor by lazy { NetworkMonitor(application).also { it.start() } }
    /** 설정(Wi-Fi 전용, 자막 자동 표시). 설정 화면이 구독한다. */
    internal val settings: StateFlow<AppSettings> get() = settingsRepository.settings
    /** 지금 연결 종류(Wi-Fi / 모바일 / 데이터 절약 / 없음). 준비 화면 세부 정보에 쓴다. */
    internal val connection: StateFlow<ConnectionType> get() = networkMonitor.connection
    /** 이번 앱 실행에서 모바일 데이터 안내를 이미 확인했는지. 앱을 다시 켜면 다시 묻는다. */
    private var mobileNoticeSeen = false
    /** "이번만 허용"으로 시작한 재생 요청([requestVersion]). 그 다운로드 작업은 모바일 데이터에서도 이어 받는다. */
    private var mobileAllowedRequest = -1L

    init {
        viewModelScope.launch {
            try { settingsRepository.load() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { Log.w("Settings", "Unable to load: ${EngineFailureLog.describe(failure)}") }
            combine(settingsRepository.settings, networkMonitor.connection) { saved, connection ->
                NetworkConditions(saved.wifiOnly, connection)
            }.distinctUntilChanged().collect(downloads::applyNetwork)
        }
    }

    fun setWifiOnly(enabled: Boolean) = updateSettings { it.copy(wifiOnly = enabled) }

    fun setAutoSubtitles(enabled: Boolean) = updateSettings { it.copy(autoSubtitles = enabled) }

    private fun updateSettings(change: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            try { settingsRepository.update(change) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { Log.w("Settings", "Unable to save: ${EngineFailureLog.describe(failure)}") }
        }
    }

    /**
     * "바로 재생" 전에 부른다. [PlaybackNetworkCheck.CONFIRM_MOBILE]이면 확인 창을 띄우고, 사용자가 계속하면
     * `playFromMain(input, allowMobileOnce = true)`(또는 재생 목록에서 `playFromPlaylist`)로 시작한다.
     */
    internal fun playbackNetworkCheck(): PlaybackNetworkCheck = DownloadNetworkPolicy.playbackStart(
        NetworkConditions(settings.value.wifiOnly, connection.value), mobileNoticeSeen)

    /** 방금 시작한 재생 요청에 모바일 데이터를 이번만 허용한다. */
    private fun allowMobileForCurrentRequest() {
        mobileNoticeSeen = true
        mobileAllowedRequest = requestVersion
    }

    fun stopDownload(key: String) {
        stopDownloads(setOf(key))
    }
    suspend fun queueDownload(input: String) {
        val resolved = withContext(Dispatchers.IO) { torrentEngine.resolveInput(input.trim()) }
        downloads.start(resolved) { true }
    }
    suspend fun resetPlaybackPosition(magnet: String) = positionWrites.withLock {
        withContext(Dispatchers.IO) { playlist.resetPosition(magnet) }
    }
    /** 준비 화면의 "재생 목록": 재생은 멈추고 다운로드는 이어 가며 재생 목록으로 간다. */
    fun continueInPlaylist() {
        continueDownloading = true
        if (sharedPlayback != null) stopStream()
        else { _activeKey.value = null; setStream(StreamUiState.Idle) }
        navigator.showPlaylistAfterPlayback()
    }

    /** 메인에서 링크를 재생한다. 닫으면 메인으로 돌아온다. */
    fun playFromMain(input: String, allowMobileOnce: Boolean = false) {
        navigator.startPlayback(PlaybackOrigin.MAIN)
        startStreaming(input)
        if (allowMobileOnce) allowMobileForCurrentRequest()
    }

    /** 재생 목록에서 재생한다. 닫거나 뒤로 가면 재생 목록으로 돌아온다. */
    fun playFromPlaylist(magnet: String, allowMobileOnce: Boolean = false) {
        navigator.startPlayback(PlaybackOrigin.PLAYLIST)
        startStreaming(magnet)
        if (allowMobileOnce) allowMobileForCurrentRequest()
    }

    /** 오류가 남은 메인에서 열면 오류 상태를 정리한 뒤 연다. */
    fun openPlaylist() {
        if (_uiState.value is StreamUiState.Error) stopStream()
        navigator.openPlaylist()
    }

    fun openStoredFiles() {
        stopStream()
        navigator.openStoredFiles()
    }

    /** 오픈소스 라이선스를 연다. 진입점(메인 하단 링크, design 시안 A)은 개인정보처리방침 주소가 정해진 뒤 연결한다. */
    fun openOpenSourceLicenses() = navigator.openOpenSourceLicenses()

    /** 재생 목록·저장 파일 관리·오픈소스 라이선스에서 뒤로 간다. */
    fun closeList() = navigator.closeList()

    /**
     * 재생·준비·오류 화면을 닫는다. 재생 목록에서 시작했으면 다운로드를 이어 가며 재생 목록으로,
     * 메인에서 시작했으면 그 다운로드도 멈추고 메인으로 돌아간다.
     */
    fun closePlayback() {
        when (navigator.origin) {
            PlaybackOrigin.PLAYLIST -> stopStream()
            PlaybackOrigin.MAIN -> stopForMain()
        }
        navigator.closePlayback()
    }

    /** 준비 화면의 "메인": 어디서 시작했든 다운로드를 멈추고 메인으로 간다. */
    fun cancelToMain() {
        stopForMain()
        navigator.cancelToMain()
    }

    /** 끝까지 재생하면 재생 목록으로 간다. */
    fun onPlaybackCompleted() {
        stopStream()
        navigator.showPlaylistAfterPlayback()
    }
    fun playbackPositionWriter(): (Long, Long) -> Unit {
        val magnet = resume.magnet ?: return { _, _ -> }
        return { position, duration -> resume.saved(magnet, position, duration)
            viewModelScope.launch { positionWrites.withLock {
            withContext(Dispatchers.IO) { runCatching { playlist.savePosition(magnet, position, duration) }
                .onFailure {
                    Log.w("PlaybackPosition", "Unable to save playback position: ${EngineFailureLog.describe(it)}")
                } }
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
        if (shared != null) commands.post { shared.releasePlaybackPriority() }
        _activeKey.value?.let { downloads.release(it) }
        _activeKey.value = null
        inputJob?.cancel()
        requestVersion++
        if (engine.isInitialized()) engine.value.let { idle -> commands.post { idle.stop() } }
        setStream(StreamUiState.Idle)
    }

    fun stopDownloads() {
        if (sharedPlayback != null) stopStream()
        downloads.stop()
    }
    override fun onCleared() {
        super.onCleared()
        downloads.stop()
        if (engine.isInitialized()) engine.value.let { idle -> commands.post { idle.stop() } }
        networkMonitor.stop()
    }
}
