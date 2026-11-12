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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import com.example.torrentstreamplayer.data.AppSettings
import com.example.torrentstreamplayer.data.ConnectionSource
import com.example.torrentstreamplayer.data.ConnectionType
import com.example.torrentstreamplayer.data.NetworkMonitor
import com.example.torrentstreamplayer.data.settled
import com.example.torrentstreamplayer.meta.NetworkSpec
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
import com.example.torrentstreamplayer.engine.SubtitleFile
import com.example.torrentstreamplayer.engine.SubtitleSelectionPolicy
import com.example.torrentstreamplayer.ui.navigation.AppNavigator
import com.example.torrentstreamplayer.ui.navigation.AppScreen
import com.example.torrentstreamplayer.ui.navigation.PlaybackOrigin

/** @param connections 연결 감시 포트. 앱은 [NetworkMonitor], 테스트는 가짜를 넣는다. */
class MainViewModel @JvmOverloads internal constructor(
    application: Application,
    private val connections: ConnectionSource = NetworkMonitor.watch(application),
) : AndroidViewModel(application) {

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

    /** 재생 중인 영상과 함께 받은(해시 검증된) 자막 파일. 재생에 빌린 엔진이 없으면 비어 있다. */
    @OptIn(ExperimentalCoroutinesApi::class)
    internal val subtitleFiles: StateFlow<List<SubtitleFile>> = playbackSource
        .flatMapLatest { it?.subtitleFiles ?: flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(replayExpirationMillis = 0), emptyList())

    /** 자동으로 켤 자막: 영상 이름과 맞는 것 중 한국어 → 영어 → 첫 번째([SubtitleSelectionPolicy]). */
    internal val preferredSubtitle: StateFlow<SubtitleFile?> = subtitleFiles
        .map(SubtitleSelectionPolicy::preferred)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(replayExpirationMillis = 0), null)

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
                if (request == requestVersion && needsMobileConfirm(resolved, request)) {
                    if (request == requestVersion) askMobilePlayback(resolved)
                    return@launch
                }
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
    /** 설정(Wi-Fi 전용, 자막 자동 표시). 설정 화면이 구독한다. */
    internal val settings: StateFlow<AppSettings> get() = settingsRepository.settings
    /** 지금 연결 종류(Wi-Fi / 모바일 / 데이터 절약 / 없음). 설정 화면 안내 줄에 쓴다. */
    internal val connection: StateFlow<ConnectionType> get() = connections.connection
    /** "이번만 허용"으로 시작한 재생 요청([requestVersion]). 그 다운로드 작업은 모바일 데이터에서도 이어 받는다. */
    private var mobileAllowedRequest = -1L
    private val _mobilePlaybackConfirm = MutableStateFlow<MobilePlaybackRequest?>(null)

    /**
     * 모바일 데이터 재생 확인 창에 올릴 요청. null이 아니면 확인 창을 띄우고 [confirmMobilePlayback] 또는
     * [dismissMobilePlayback]을 부른다. 모든 재생 진입점([playFromMain], [playFromPlaylist])이 여기를 거친다.
     */
    internal val mobilePlaybackConfirm: StateFlow<MobilePlaybackRequest?> = _mobilePlaybackConfirm.asStateFlow()

    /** Wi-Fi 전용인데 Wi-Fi가 끊겨 모바일 데이터로 이어 재생 중인 작업 키. null이 아니게 바뀌면 알림을 한 번 보인다. */
    internal val mobilePlayback: StateFlow<String?> get() = downloads.mobilePlayback

    init {
        viewModelScope.launch {
            try { settingsRepository.load() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { Log.w("Settings", "Unable to load: ${EngineFailureLog.describe(failure)}") }
            combine(settingsRepository.settings, connections.connection.settled(NetworkSpec.CONNECTION_SETTLE_MS)) {
                saved, connection -> NetworkConditions(saved.wifiOnly, connection)
            }.distinctUntilChanged().collect(downloads::applyNetwork)
        }
    }

    /** Wi-Fi 전용 + 모바일 데이터에서 "이번만 허용" 없이 덜 받은 영상을 재생하려 하면 true. */
    private suspend fun needsMobileConfirm(magnet: String, request: Long): Boolean {
        if (continueDownloading || request == mobileAllowedRequest) return false
        val key = MagnetIdentity.key(magnet)
        if (downloads.playbackCheck(key, complete = false) == PlaybackNetworkCheck.START) return false
        val complete = withContext(Dispatchers.IO) {
            runCatching { PlaylistOrdering.complete(torrentEngine.savedMagnet(SavedMagnet(magnet, "", 0))) }
                .getOrDefault(false)
        }
        return downloads.playbackCheck(key, complete) == PlaybackNetworkCheck.CONFIRM_MOBILE
    }

    /** 재생을 시작하지 않고 시작한 화면으로 돌아가 확인 창을 띄운다. */
    private fun askMobilePlayback(magnet: String) {
        val origin = navigator.origin
        _activeKey.value = null
        setStream(StreamUiState.Idle)
        navigator.closePlayback()
        _mobilePlaybackConfirm.value = MobilePlaybackRequest(magnet, origin)
    }

    /** 확인 창의 "이번만 재생": 같은 곳에서 이번만 모바일 데이터를 허용해 다시 시작한다. */
    fun confirmMobilePlayback() {
        val request = _mobilePlaybackConfirm.value ?: return
        _mobilePlaybackConfirm.value = null
        when (request.origin) {
            PlaybackOrigin.MAIN -> playFromMain(request.magnet, allowMobileOnce = true)
            PlaybackOrigin.PLAYLIST -> playFromPlaylist(request.magnet, allowMobileOnce = true)
        }
    }

    /** 확인 창의 "취소"·바깥 탭·뒤로: 아무것도 시작하지 않는다. */
    fun dismissMobilePlayback() {
        _mobilePlaybackConfirm.value = null
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

    /** 방금 시작한 재생 요청에 모바일 데이터를 이번만 허용한다. 주소 처리 전에 불려 [needsMobileConfirm]이 본다. */
    private fun allowMobileForCurrentRequest() {
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

    /** 다른 앱 링크 → 확인 → 재생/받기 흐름. ViewModel은 참여 함수만 넘긴다. */
    private val incomingLinks = IncomingLinkCoordinator(
        scope = viewModelScope,
        playlistKeys = {
            try { withContext(Dispatchers.IO) { playlist.keys() } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) {
                Log.w("IncomingLink", "Unable to read playlist: ${EngineFailureLog.describe(failure)}")
                emptySet()
            }
        },
        isPlaybackActive = { _uiState.value !is StreamUiState.Idle && _uiState.value !is StreamUiState.Error },
        startPlayback = { link -> playFromMain(link) },
        saveLink = { link -> queueDownload(link) },
    )

    /** 링크 시트가 보일 것: 확인(허용 동작 포함) 또는 열 수 없는 이유. 시트가 없으면 null. */
    internal val incomingLink: StateFlow<IncomingLinkState?> get() = incomingLinks.state

    /** 다른 앱이 넘긴 링크(또는 열 수 없는 이유)를 시트에 올린다. 사용자가 고르기 전에는 아무것도 시작하지 않는다. */
    internal fun receiveLink(result: IncomingLinkResult) = incomingLinks.receive(result)

    /** 시트의 "바로 재생". 재생·준비 중이면 허용되지 않아 무시하고 false를 돌려준다. */
    fun playIncomingLink(): Boolean = incomingLinks.play()

    /** 시트의 "받아 두기". `.torrent` 주소 처리에 실패하면 예외를 던지고 시트는 남는다([IncomingLinkCoordinator.save]). */
    suspend fun saveIncomingLink() = incomingLinks.save()

    /** 시트의 "취소"·오류 시트의 "확인". */
    fun dismissIncomingLink() = incomingLinks.dismiss()

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
        connections.stop()
    }
}
