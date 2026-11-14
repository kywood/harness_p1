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
import com.example.torrentstreamplayer.R
import com.example.torrentstreamplayer.data.AppSettings
import com.example.torrentstreamplayer.data.ConnectionSource
import com.example.torrentstreamplayer.data.ConnectionType
import com.example.torrentstreamplayer.data.NetworkMonitor
import com.example.torrentstreamplayer.data.settled
import com.example.torrentstreamplayer.meta.NetworkSpec
import com.example.torrentstreamplayer.meta.TorrentFailure
import com.example.torrentstreamplayer.data.PlaylistRepository
import com.example.torrentstreamplayer.data.SettingsRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import com.example.torrentstreamplayer.engine.DownloadOrderPreference
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
import java.io.File

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
    /**
     * Holds [playlistWrites] so a late queued-link record cannot bring the card back.
     * Stops [magnet] on the main thread, then waits for its writes and deletes its files on [Dispatchers.IO].
     */
    suspend fun deleteSavedMagnet(magnet: String): Unit = playlistWrites.delete {
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
        if (!commands.awaitIdle()) throw TorrentFailure.STOP_PENDING.blocked()
        if (engine.isInitialized()) engine.value.awaitStoppedWrites()
        downloads.awaitStoppedWrites(writers)
    }
    /** 받기 전용 작업의 받는 순서: 영상별 값이 있으면 그 값, 없으면 설정의 기본값. */
    private val downloadOrders = DownloadOrderLookup({ playlist.downloadOrders() },
        { settingsRepository.settings.value.downloadOrder })

    /** 설정 기본값 변경과 영상별 값 변경을 한 번에 하나씩 계산·적용한다(오래된 값이 새 값을 덮지 않게). */
    private val downloadOrderSync = DownloadOrderSync(downloadOrders) { key, order ->
        downloads.applyDownloadOrder(key, order)
    }
    internal val downloads = DownloadManager(viewModelScope, { DownloadController(viewModelScope, { TorrentEngine(application.applicationContext) },
        { withContext(Dispatchers.IO) { playlist.add(it) }; Unit },
        { magnet, name -> withContext(Dispatchers.IO) { playlist.updateTitle(magnet, name) }; Unit },
        order = downloadOrders::orderFor) },
        onTaskAdded = ::saveQueuedLink)


    /** 대기 작업 기록과 재생 목록 삭제의 순서. 작업이 살았는지는 메인 스레드에서 본다([DownloadManager]는 메인 전용). */
    private val playlistWrites = PlaylistWriteGate { key -> downloads.entry(key)?.phase?.stoppable == true }

    /**
     * 대기열·Wi-Fi 대기로 남은 작업을 재생 목록에 바로 보이게 기록한다. 바로 실행되는 작업은 컨트롤러가 기록한다.
     * 차례가 왔을 때 작업이 이미 멈췄으면(삭제·중지) 쓰지 않아, 지운 카드가 다시 생기지 않는다.
     */
    private fun saveQueuedLink(magnet: String) {
        val key = MagnetIdentity.key(magnet)
        viewModelScope.launch {
            try { playlistWrites.recordIfAlive(key) { withContext(Dispatchers.IO) { playlist.add(magnet) } } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { Log.w("Playlist", "Unable to save: ${EngineFailureLog.describe(failure)}") }
        }
    }
    /** Stopped downloads may still be writing their files; waits for them on [Dispatchers.IO] before deleting. */
    suspend fun deleteStoredFile(path: String): Boolean {
        val writers = withContext(Dispatchers.Main.immediate) {
            if (downloads.tasks.value.values.any { it.phase.stoppable }) throw TorrentFailure.STOP_DOWNLOADS_FIRST.blocked()
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

    /** 재생 중인 영상과 함께 아직 받는 중인 자막 파일. 자막 시트에 "받는 중"으로 보인다. */
    @OptIn(ExperimentalCoroutinesApi::class)
    internal val pendingSubtitleFiles: StateFlow<List<File>> = playbackSource
        .flatMapLatest { it?.pendingSubtitleFiles ?: flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(replayExpirationMillis = 0), emptyList())

    /** 자동으로 켤 자막: 영상 이름과 맞는 것 중 한국어 → 영어 → 첫 번째([SubtitleSelectionPolicy]). */
    internal val preferredSubtitle: StateFlow<SubtitleFile?> = subtitleFiles
        .map(SubtitleSelectionPolicy::preferred)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(replayExpirationMillis = 0), null)

    private var continueDownloading = false
    private val _activeKey = MutableStateFlow<String?>(null)
    val activeKey = _activeKey.asStateFlow()
    private var requestVersion = 0L

    /**
     * 재생(또는 [downloadOnly]면 받기만)을 시작한다. [allowMobileOnce]는 확인 창에서 "이번만 재생"을 고른 재생이다.
     * Wi-Fi 전용 확인은 주소를 처리한 뒤 [needsMobileConfirm]에서 한다.
     */
    fun startStreaming(magnetUrl: String, downloadOnly: Boolean = false, allowMobileOnce: Boolean = false) {
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
            setStream(StreamUiState.Error(getApplication<Application>().getString(R.string.error_empty_link)))
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
                if (request == requestVersion && needsMobileConfirm(resolved, request, allowMobileOnce)) {
                    if (request == requestVersion) askMobilePlayback(resolved)
                    return@launch
                }
                if (request == requestVersion) startResolved(resolved, request)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                if (request == requestVersion)
                    setStream(StreamUiState.Error(InputFailureText.message(e, getApplication())
                        ?: getApplication<Application>().getString(R.string.error_input_failed)))
            }
        }
    }

    private fun startResolved(magnet: String, request: Long) {
        val key = com.example.torrentstreamplayer.engine.MagnetIdentity.key(magnet)
        downloads.start(magnet) { true }
        if (continueDownloading) { _activeKey.value = null; setStream(StreamUiState.Idle); return }
        val source = downloads.borrow(key) ?: throw TorrentFailure.TASK_UNAVAILABLE.blocked()
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
                    val app = getApplication<Application>()
                    val message = task?.error?.text(app) ?: app.getString(R.string.error_download_stopped)
                    setStream(StreamUiState.Error(message))
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

    /**
     * [NetworkSpec.CONNECTION_SETTLE_MS] 동안 같았던 연결. 다운로드 규칙([DownloadManager.applyNetwork])과 [waitsForWifi]가
     * 같은 값을 써서, 안내 줄·스낵바와 실제 Wi-Fi 대기 상태가 어긋나지 않는다.
     */
    private val settledConnection: StateFlow<ConnectionType> by lazy {
        connections.connection.settled(NetworkSpec.CONNECTION_SETTLE_MS)
            .stateIn(viewModelScope, SharingStarted.Eagerly, connections.connection.value)
    }

    /** 지금 새 다운로드가 "Wi-Fi 대기"로 멈추면 true([WifiWaitPolicy]). 설정 안내 줄과 받기 시작 스낵바가 쓴다. */
    internal val waitsForWifi: StateFlow<Boolean> by lazy {
        combine(settingsRepository.settings, settledConnection) { saved, connection ->
            WifiWaitPolicy.waitsForWifi(saved.wifiOnly, connection)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    }
    private val mobileGate = MobilePlaybackGate()

    /**
     * 모바일 데이터 재생 확인 창에 올릴 요청. null이 아니면 확인 창을 띄우고 [confirmMobilePlayback] 또는
     * [dismissMobilePlayback]을 부른다. 모든 재생 진입점([playFromMain], [playFromPlaylist])이 여기를 거친다.
     */
    internal val mobilePlaybackConfirm: StateFlow<MobilePlaybackRequest?> get() = mobileGate.request

    /** Wi-Fi 전용인데 Wi-Fi가 끊겨 모바일 데이터로 이어 재생 중인 작업 키. null이 아니게 바뀌면 알림을 한 번 보인다. */
    internal val mobilePlayback: StateFlow<String?> get() = downloads.mobilePlayback

    init {
        viewModelScope.launch {
            try { settingsRepository.load() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { Log.w("Settings", "Unable to load: ${EngineFailureLog.describe(failure)}") }
            combine(settingsRepository.settings, settledConnection) {
                saved, connection -> NetworkConditions(saved.wifiOnly, connection)
            }.distinctUntilChanged().collect(downloads::applyNetwork)
        }
        // 설정의 받는 순서가 바뀌면(처음 읽은 값 포함) 영상별 값을 정하지 않은 진행 중 작업에도 바로 적용한다.
        viewModelScope.launch {
            settingsRepository.settings.map { it.downloadOrder }.distinctUntilChanged().collect { applyDownloadOrders() }
        }
    }

    /**
     * Wi-Fi 전용 + 모바일 데이터에서 "이번만 허용" 없이 덜 받은 영상을 재생하려 하면 true. 판단과 허용 기록은
     * [DownloadManager.preparePlayback] 한 곳에서 한다(다 받은 파일·이번만 재생은 허용이 남아 [startResolved]가 빌린다).
     * 연결 변화는 [NetworkSpec.CONNECTION_SETTLE_MS] 뒤에 반영되므로, 그 사이에 시작한 재생은 묻지 않고 시작해
     * [mobilePlayback]으로 알린다.
     */
    private suspend fun needsMobileConfirm(magnet: String, request: Long, allowMobileOnce: Boolean): Boolean {
        if (continueDownloading) return false
        val key = MagnetIdentity.key(magnet)
        if (downloads.preparePlayback(key, complete = false, allowMobileOnce) == PlaybackNetworkCheck.START) return false
        val complete = withContext(Dispatchers.IO) {
            runCatching { PlaylistOrdering.complete(torrentEngine.savedMagnet(SavedMagnet(magnet, "", 0))) }
                .getOrDefault(false)
        }
        if (request != requestVersion || continueDownloading) return false
        return downloads.preparePlayback(key, complete, allowMobileOnce = false) == PlaybackNetworkCheck.CONFIRM_MOBILE
    }

    /** 재생을 시작하지 않고 시작한 화면으로 돌아가 확인 창을 띄운다. */
    private fun askMobilePlayback(magnet: String) {
        val origin = navigator.origin
        _activeKey.value = null
        setStream(StreamUiState.Idle)
        navigator.closePlayback()
        mobileGate.ask(MobilePlaybackRequest(magnet, origin))
    }

    /** 확인 창의 "이번만 재생": 같은 곳에서 이번만 모바일 데이터를 허용해 다시 시작한다. */
    fun confirmMobilePlayback() {
        val request = mobileGate.accept() ?: return
        when (request.origin) {
            PlaybackOrigin.MAIN -> playFromMain(request.magnet, allowMobileOnce = true)
            PlaybackOrigin.PLAYLIST -> playFromPlaylist(request.magnet, allowMobileOnce = true)
        }
    }

    /** 확인 창의 "취소"·바깥 탭·뒤로: 아무것도 시작하지 않는다. */
    fun dismissMobilePlayback() = mobileGate.clear()

    fun setWifiOnly(enabled: Boolean) = updateSettings { it.copy(wifiOnly = enabled) }

    /**
     * 설정 화면 스위치: 저장된 값 기준으로 뒤집는다. 저장이 끝나야 스위치가 바뀌므로, 저장 중에 다시 누른 탭도
     * 사라지지 않고 한 번씩 반영된다([SettingsRepository.update]가 차례로 처리).
     */
    fun toggleWifiOnly() = updateSettings { it.copy(wifiOnly = !it.wifiOnly) }

    /** 설정의 "받는 순서" 기본값. 저장에 성공해야 값이 바뀌고, 바뀌면 [applyDownloadOrders]가 진행 중 작업에 적용한다. */
    fun setDownloadOrder(order: DownloadOrderPreference) = updateSettings { it.copy(downloadOrder = order) }

    /**
     * 재생 목록 카드의 "받는 순서": [magnet] 영상의 값을 저장하고(null이면 기본 설정 따름) 받는 중인 작업에 바로 적용한다.
     * 재생 중인 작업은 재생이 끝날 때 적용된다. 저장에 실패하면 예외를 던지고 아무것도 바꾸지 않는다.
     */
    suspend fun setVideoDownloadOrder(magnet: String, order: DownloadOrderPreference?) {
        val key = MagnetIdentity.key(magnet)
        withContext(Dispatchers.Main.immediate) {
            downloadOrderSync.sync({ listOf(key) }, save = { playlist.setDownloadOrder(magnet, order) })
        }
    }

    /** 살아 있는 작업마다 받는 순서를 다시 정해 엔진에 넘긴다. 엔진 호출은 [DownloadController.applyOrder]가 큐에 넣는다. */
    private suspend fun applyDownloadOrders() = downloadOrderSync.sync({ downloads.tasks.value.keys })

    fun toggleAutoSubtitles() = updateSettings { it.copy(autoSubtitles = !it.autoSubtitles) }

    fun setAutoSubtitles(enabled: Boolean) = updateSettings { it.copy(autoSubtitles = enabled) }

    /**
     * 재생 화면에서 고른 자막 언어를 "선호 언어"로 기억한다(영상별이 아님, PR #131 PM 결정). 다른 설정과 같이
     * [SettingsRepository.update]가 저장에 성공해야 값이 바뀐다. 실패는 로그만 남긴다: 사용자가 누른 스위치가 아니라
     * 설정 화면의 "다시 눌러 주세요" 안내([settingsSaveFailed])를 띄우지 않고, 다음에 고를 때 다시 저장한다.
     */
    fun rememberSubtitleLanguage(key: String) {
        viewModelScope.launch {
            try { settingsRepository.update { it.copy(subtitleLanguage = key) } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { Log.w("Settings", "Unable to save: ${EngineFailureLog.describe(failure)}") }
        }
    }
    private val saveNotice = SettingsSaveNotice()

    /** 설정을 디스크에 저장하지 못했으면 true. 설정 화면이 스낵바를 한 번 보이고 [consumeSettingsSaveFailed]를 부른다. */
    internal val settingsSaveFailed: StateFlow<Boolean> get() = saveNotice.failed

    fun consumeSettingsSaveFailed() = saveNotice.consume()

    private fun updateSettings(change: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            try { settingsRepository.update(change) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) {
                Log.w("Settings", "Unable to save: ${EngineFailureLog.describe(failure)}")
                saveNotice.report()
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
        startStreaming(input, allowMobileOnce = allowMobileOnce)
    }

    /** 재생 목록에서 재생한다. 닫거나 뒤로 가면 재생 목록으로 돌아온다. */
    fun playFromPlaylist(magnet: String, allowMobileOnce: Boolean = false) {
        navigator.startPlayback(PlaybackOrigin.PLAYLIST)
        startStreaming(magnet, allowMobileOnce = allowMobileOnce)
    }

    /** 다른 앱 링크 → 확인 → 재생/받기 흐름. ViewModel은 참여 함수만 넘긴다. */
    private val incomingLinks = IncomingLinkCoordinator(
        scope = viewModelScope,
        // 읽기 실패는 코디네이터가 "재생 목록에 없음"으로 처리한다.
        findInPlaylist = { key ->
            withContext(Dispatchers.IO) { playlist.find(key) }?.let { entry ->
                PlaylistMatch(entry.magnet, entry.title, entry.lastPositionMs, PlaylistOrdering.complete(entry))
            }
        },
        isPlaybackActive = { _uiState.value !is StreamUiState.Idle && _uiState.value !is StreamUiState.Error },
        // Wi-Fi 전용 확인은 playFromMain 안([needsMobileConfirm] → [mobilePlaybackConfirm])에서 한다.
        startPlayback = { link -> playFromMain(link) },
        saveLink = { link -> queueDownload(link) },
    )

    /** 링크 시트가 보일 것: 확인(허용 동작 포함) 또는 열 수 없는 이유. 시트가 없으면 null. */
    internal val incomingLink: StateFlow<IncomingLinkState?> get() = incomingLinks.state

    /** 다른 앱이 넘긴 링크(또는 열 수 없는 이유)를 시트에 올린다. 사용자가 고르기 전에는 아무것도 시작하지 않는다. */
    internal fun receiveLink(result: IncomingLinkResult) = incomingLinks.receive(result)

    /** 시트의 "바로 재생". 재생·준비 중이면 허용되지 않아 무시하고 false를 돌려준다. */
    fun playIncomingLink(): Boolean = incomingLinks.play()

    /** 시트의 "받아 두기". 끝나면 시트를 닫고 결과를 [incomingLinkNotice]로 알린다([IncomingLinkCoordinator.save]). */
    fun saveIncomingLink() {
        viewModelScope.launch { incomingLinks.save() }
    }

    /** 시트의 "취소"·오류 시트의 "확인". [shown]이 아직 시트의 링크일 때만 닫는다(닫는 애니메이션 중 새 링크 보호). */
    internal fun dismissIncomingLink(shown: IncomingLinkState) = incomingLinks.dismiss(shown)

    /** "받아 두기" 뒤 한 번 보일 스낵바. 스낵바가 끝난 뒤 [incomingLinkNoticeShown]을 부른다. */
    internal val incomingLinkNotice: StateFlow<IncomingLinkNoticeEvent?> get() = incomingLinks.notice

    internal fun incomingLinkNoticeShown(event: IncomingLinkNoticeEvent) = incomingLinks.noticeShown(event)

    /** 들어온 링크로 메인 입력란을 채우거나, 받아 둔 뒤 비운다. 메인이 반영한 뒤 [mainInputFilled]를 부른다. */
    internal val mainInputFill: StateFlow<MainInputFill?> get() = incomingLinks.mainInput

    internal fun mainInputFilled(fill: MainInputFill) = incomingLinks.inputFilled(fill)

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

    /** 메인 오른쪽 위 톱니: 설정을 연다. */
    fun openSettings() {
        saveNotice.clear()
        navigator.openSettings()
    }

    /** 재생 목록·저장 파일 관리·오픈소스 라이선스·설정에서 뒤로 간다. */
    fun closeList() {
        if (screen.value == AppScreen.Settings) saveNotice.clear()
        navigator.closeList()
    }

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
    /** 재생을 멈춘다. 새 재생도 이것으로 시작하므로 남아 있던 모바일 데이터 확인 요청도 여기서 지운다. */
    fun stopStream() {
        mobileGate.clear()
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
