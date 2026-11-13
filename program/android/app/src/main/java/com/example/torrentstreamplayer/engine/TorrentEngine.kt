package com.example.torrentstreamplayer.engine

import android.content.Context
import android.os.StrictMode
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.example.torrentstreamplayer.BuildConfig
import org.libtorrent4j.Priority
import org.libtorrent4j.SessionManager
import org.libtorrent4j.SessionParams
import org.libtorrent4j.SettingsPack
import org.libtorrent4j.Address
import org.libtorrent4j.swig.settings_pack
import org.libtorrent4j.swig.ip_filter
import org.libtorrent4j.TorrentFlags
import org.libtorrent4j.TorrentHandle
import org.libtorrent4j.TorrentInfo
import org.libtorrent4j.TorrentStatus
import java.io.File
import com.example.torrentstreamplayer.meta.EngineSpec
import com.example.torrentstreamplayer.meta.TorrentFailure
import com.example.torrentstreamplayer.meta.TorrentStorageDir
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class TorrentEngine(private val context: Context) {

    private var sessionManager: SessionManager? = null
    @Volatile private var currentHandle: TorrentHandle? = null
    @Volatile private var activeRun = 0L
    /** Run whose file and edge priorities are set; playback priorities set earlier would be overwritten by them. */
    @Volatile private var prioritiesSetRun = -1L
    @Volatile var localVideo: File? = null
        private set
    private var worker: Thread? = null
    private val retiredWorkers = java.util.concurrent.CopyOnWriteArrayList<Thread>()
    private val closingCheckpoints = java.util.concurrent.CopyOnWriteArrayList<TorrentCheckpoint>()
    internal val isDownloading: Boolean get() = currentHandle != null && sessionManager?.isRunning == true && worker?.isAlive == true
    internal val nativeSessionIdentity: Any? get() = sessionManager
    /** True while the selected torrent is active in the session and could be advertised to peers. */
    internal val isSharingTorrent: Boolean get() = runCatching {
        currentHandle?.let { !it.flags.and_(TorrentFlags.PAUSED).non_zero() } ?: false }.getOrDefault(false)
    /** For tests: whether the selected torrent downloads in native sequential order ([DownloadOrderPolicy]). */
    @VisibleForTesting internal val sequentialDownload: Boolean get() = runCatching {
        currentHandle?.let { it.flags.and_(TorrentFlags.SEQUENTIAL_DOWNLOAD).non_zero() } ?: false }.getOrDefault(false)
    /** For tests: file-end pieces still fetched early ([StartupPrefetchPlan]) and the currently pinned pieces. */
    @VisibleForTesting internal val tailPrefetchPieces: Set<Int> get() = prefetchPieces
    @VisibleForTesting internal val pinnedPieces: Set<Int> @Synchronized get() = priorityPins
    /** For tests: the native priority (0-7) of [piece], or null without a torrent. */
    @VisibleForTesting internal fun piecePriorityOf(piece: Int): Int? = runCatching { currentHandle?.piecePriority(piece)?.swig()?.toInt() }.getOrNull()
    internal val uploadedBytes: Long get() = runCatching { currentHandle?.status()?.allTimeUpload() ?: 0L }.getOrDefault(0L)
    private var checkpoint: TorrentCheckpoint? = null
    private var activeCache: TorrentCache? = null
    internal val seekRequests = SeekRequestTracker()
    private var priorityWindow = IntRange.EMPTY
    private var backgroundPiecePriority = -1
    private var playbackByteOffset = 0L
    @Volatile private var playbackDurationMs = 0L
    @Volatile private var startupPieces: Set<Int> = emptySet()
    private var startupIndex: StartupIndexState? = null
    private val readDemands = ReadDemandRegistry()
    private var priorityPins: Set<Int> = emptySet()
    @Volatile private var startTrace: PlaybackStartTrace? = null
    /** File-end index pieces pinned like the startup buffer but not waited for ([StartupPrefetchPlan]). */
    @Volatile private var prefetchPieces: Set<Int> = emptySet()
    private val _pieceMap = MutableStateFlow(PieceMapSnapshot.EMPTY)
    val pieceMap = _pieceMap.asStateFlow()
    private val _metadataDetails = MutableStateFlow(MetadataDetails())
    internal val metadataDetails = _metadataDetails.asStateFlow()
    private val subtitles = SubtitleDownloads()
    /** Verified subtitle files downloaded with the selected video (path, guessed language). */
    internal val subtitleFiles = subtitles.files
    /** Chosen subtitle files still downloading. */
    internal val pendingSubtitleFiles = subtitles.pending
    private val _activeDownload = MutableStateFlow<DownloadSnapshot?>(null)
    internal val activeDownload = _activeDownload.asStateFlow()
    private val storage = TorrentStorage.of(context)
    private val downloadRoot = storage.dir(TorrentStorageDir.DOWNLOADS)
    private val stateRoot = storage.dir(TorrentStorageDir.STATE)
    private val diskStore = TorrentDiskStore(listOf(downloadRoot, stateRoot))
    private val teardown = EngineTeardown(onError = { Log.e(TAG, "Stop failed: ${EngineFailureLog.describe(it)}") })
    var selectedVideoFile: File? = null
        private set
    var selectedFileLength: Long = 0L
        private set
    var selectedFileStartOffset: Long = 0L
        private set
    var firstPieceIndex: Int = -1
        private set
    var lastPieceIndex: Int = -1
        private set

    var downloadProgress: Float = 0f
        private set
    var downloadSpeedBytes: Long = 0L
        private set
    var numPeers: Int = 0
        private set

    companion object {
        private const val TAG = "TorrentEngine"
        private val sharedSessions = NativeSessionLease<SessionManager> { it.stop() }
    }

    @Synchronized
    private fun ensureStarted(run: Long): SessionManager {
        check(run == activeRun) { "Stream was cancelled" }
        val existing = sessionManager
        if (existing != null && existing.isRunning) {
            return existing
        }
        val newSession = sharedSessions.acquire(this) {
        val nativeSession = SessionManager()
        val settings = SettingsPack()
        settings.setInteger(settings_pack.int_types.initial_picker_threshold.swigValue(), 0)
        DownloadOnlyPolicy.settings(settings_pack.int_types.choking_algorithm,
            settings_pack.int_types.unchoke_slots_limit, settings_pack.int_types.allowed_fast_set_size,
            settings_pack.choking_algorithm_t.fixed_slots_choker.swigValue()
        ).forEach { (key, value) -> settings.setInteger(key.swigValue(), value) }
        nativeSession.start(SessionParams(settings))
        try {
            val peerClasses = ip_filter()
            DownloadOnlyPolicy.assignPeerClasses { first, last, mask ->
                peerClasses.add_rule(Address.parseIp(first).swig(), Address.parseIp(last).swig(), mask)
            }
            nativeSession.swig().set_peer_class_filter(peerClasses)
        } catch (t: Throwable) {
            nativeSession.stop()
            throw t
        }
        nativeSession
        }
        sessionManager = newSession
        return newSession
    }

    @Synchronized fun startStreaming(
        magnetUrl: String,
        onMetadataReceived: (fileName: String, fileSize: Long) -> Unit,
        onProgress: (progress: Float, downloadSpeedBytes: Long) -> Unit,
        onError: (String) -> Unit,
        prioritizePlayback: Boolean = true
    ) {
        StrictMode.noteSlowCall("TorrentEngine.startStreaming")
        val key = try { MagnetIdentity.key(magnetUrl) }
        catch (_: Exception) { onError(TorrentFailure.INVALID_MAGNET.text(context)); return }
        stop()
        val run = activeRun
        startTrace = if (BuildConfig.DEBUG) PlaybackStartTrace(key) else null
        val cache = TorrentCache(stateRoot, key, TorrentCacheCipher::encrypt, TorrentCacheCipher::decrypt)
        activeCache = cache
        var downloadDir = File(downloadRoot, key)
        if (!downloadDir.exists()) {
            downloadDir.mkdirs()
        }

        worker = Thread {
            try {
                check(teardown.await()) { "Previous download is still stopping" }
                if (BuildConfig.DEBUG) Log.d(TAG, "Fetching magnet metadata...")
                val cachedMetadata = cache.read("metadata")
                _metadataDetails.value = MetadataDetails(phase = if (cachedMetadata == null)
                    "네트워크 세션 준비 중…" else "저장된 토렌트 정보 확인 중…",
                    source = if (cachedMetadata == null) "네트워크" else "저장된 메타데이터")
                val (metadataBytes, session) = try {
                    MetadataStartup.load(cachedMetadata, { ensureStarted(run) }, { Thread.sleep(500) }) { session ->
                        MetadataMonitor(session) { if (run == activeRun) _metadataDetails.value = it }
                            .fetch(magnetUrl, downloadDir)
                    }
                } catch (t: Throwable) {
                    Log.e(TAG, "Error in fetchMagnet: ${EngineFailureLog.describe(t)}")
                    null to null
                }

                if (run != activeRun) return@Thread
                if (metadataBytes == null || metadataBytes.isEmpty()) {
                    onError(TorrentFailure.METADATA_TIMEOUT.text(context))
                    return@Thread
                }

                val torrentInfo = try {
                    TorrentInfo.bdecode(metadataBytes)
                } catch (t: Throwable) {
                    Log.e(TAG, "Error in TorrentInfo.bdecode: ${EngineFailureLog.describe(t)}")
                    null
                }

                if (torrentInfo == null) {
                    onError(TorrentFailure.METADATA_DECODE.text(context))
                    return@Thread
                }

                val fileStorage = torrentInfo.files()
                _metadataDetails.value = _metadataDetails.value.received(torrentInfo.name(), fileStorage.numFiles(),
                    torrentInfo.totalSize(), torrentInfo.numPieces(), torrentInfo.pieceLength().toLong())
                runCatching { cache.write("metadata", metadataBytes) }
                val numFiles = fileStorage.numFiles()

                var bestFileIndex = -1
                var maxFileSize = -1L

                for (i in 0 until numFiles) {
                    val filePath = fileStorage.filePath(i)
                    val fileSize = fileStorage.fileSize(i)

                    if (TorrentPlayback.isVideo(filePath, fileSize)) {
                        if (fileSize > maxFileSize) {
                            maxFileSize = fileSize
                            bestFileIndex = i
                        }
                    }
                }

                if (bestFileIndex == -1) {
                    onError(TorrentFailure.NO_VIDEO.text(context))
                    return@Thread
                }

                var startOffset = 0L
                for (i in 0 until bestFileIndex) {
                    startOffset += fileStorage.fileSize(i)
                }

                val selectedRelativePath = fileStorage.filePath(bestFileIndex)
                if (run != activeRun) return@Thread
                val legacy = File(downloadRoot, selectedRelativePath).canonicalFile
                if (legacy.toPath().startsWith(downloadRoot.canonicalFile.toPath()) && legacy.isFile)
                    downloadDir = downloadRoot
                val candidate = File(downloadDir, selectedRelativePath).canonicalFile
                require(candidate.toPath().startsWith(downloadDir.canonicalFile.toPath())) {
                    "Torrent file path escapes download directory"
                }
                synchronized(this) {
                if (run != activeRun) return@Thread
                ActiveTorrentFiles.acquire(this, candidate)
                subtitles.select(torrentInfo, bestFileIndex, downloadDir)
                selectedFileStartOffset = startOffset
                selectedFileLength = maxFileSize
                selectedVideoFile = candidate
                val selectedRange = getFilePieceRange(torrentInfo, bestFileIndex)
                firstPieceIndex = selectedRange.first
                lastPieceIndex = selectedRange.second
                startupPieces = StartupBufferPlan.required(maxFileSize, startOffset,
                    torrentInfo.pieceLength().toLong(), candidate.extension).takeIf { prioritizePlayback } ?: emptySet()
                startupIndex = StartupIndexState(candidate, maxFileSize, startOffset,
                    torrentInfo.pieceLength().toLong(), candidate.extension).takeIf { prioritizePlayback }
                if (BuildConfig.DEBUG) Log.d(TAG, "Initial playback requires ${startupPieces.size} verified pieces")
                _pieceMap.value = PieceMapSnapshot.capture(firstPieceIndex, lastPieceIndex) { false }
                if (cache.isComplete(candidate, maxFileSize)) {
                    _pieceMap.value = PieceMapSnapshot.capture(firstPieceIndex, lastPieceIndex) { true }
                    localVideo = candidate
                    downloadProgress = 100f
                    _activeDownload.value = DownloadSnapshot(maxFileSize, maxFileSize, candidate.path, _pieceMap.value)
                    _metadataDetails.value = _metadataDetails.value.copy(downloadedBytes = maxFileSize, videoSize = maxFileSize)
                    runCatching { cache.write("progress", _activeDownload.value!!.encode()) }
                    subtitles.restore(cache.read("subtitles"))
                    onMetadataReceived(candidate.name, maxFileSize)
                    onProgress(100f, 0L)
                    return@Thread
                }

                // Never log the file name or path: logcat would expose what the user watches.
                if (BuildConfig.DEBUG) Log.d(TAG, "Selected video: $maxFileSize bytes at offset $selectedFileStartOffset")
                }

                if (run != activeRun) return@Thread
                val activeSession = session ?: ensureStarted(run)
                val handle = synchronized(this) {
                    check(run == activeRun) { "Stream was cancelled" }
                    TorrentCheckpoint.restore(activeSession, torrentInfo, downloadDir, cache, bestFileIndex).also {
                        currentHandle = it
                        checkpoint = TorrentCheckpoint(activeSession, it, cache)
                    }
                }

                var lastCheckpoint = System.nanoTime()
                var lastProgressSave = 0L
                val selectedPieceLength = torrentInfo.pieceLength().toLong()
                val downloadCheckpoint = checkpoint

                // Download-only tasks read from the file start; playback's deadline windows need native sequential off.
                try {
                    applyDownloadOrder(handle, playbackFocused = prioritizePlayback)
                } catch (t: Throwable) {
                    Log.e(TAG, "Error setting sequential download flag: ${EngineFailureLog.describe(t)}")
                    onError(TorrentFailure.DOWNLOAD_SETUP.text(context))
                    return@Thread
                }

                // Set file priorities: IGNORE all except selected video file
                try {
                    for (i in 0 until numFiles) {
                        if (i == bestFileIndex) {
                            handle.filePriority(i, Priority.DEFAULT)
                        } else {
                            handle.filePriority(i, Priority.IGNORE)
                        }
                    }
                    subtitles.prioritize(handle)

                    val filePieceRange = getFilePieceRange(torrentInfo, bestFileIndex)
                    firstPieceIndex = filePieceRange.first
                    lastPieceIndex = filePieceRange.second

                    // Prioritize first and last pieces for video header / index metadata
                    DownloadOrderPolicy.edgePieces(firstPieceIndex, lastPieceIndex)
                        .forEach { handle.piecePriority(it, Priority.TOP_PRIORITY) }
                } catch (t: Throwable) {
                    Log.e(TAG, "Error setting piece priorities: ${EngineFailureLog.describe(t)}")
                    onError(TorrentFailure.DOWNLOAD_SETUP.text(context))
                    return@Thread
                }

                if (prioritizePlayback) prioritizeOffset(0L)
                startupPieces.filterNot { it in priorityWindow }.forEach { handle.setPieceDeadline(it, 5000) }
                prioritiesSetRun = run
                onMetadataReceived(selectedVideoFile?.name ?: "Video", selectedFileLength)
                _metadataDetails.value = _metadataDetails.value.copy(phase = if (prioritizePlayback)
                    "초기 재생용 조각 확보 중…" else "파일 다운로드 중…")

                // Progress polling loop
                while (run == activeRun && activeSession.isRunning && currentHandle != null) {
                    try {
                        val status = handle.status(TorrentHandle.QUERY_PIECES)
                        updateStartupIndex()
                        advanceDownloadWindow()
                        val pieces = status.pieces()
                        if (BuildConfig.DEBUG) startTrace?.observe(firstPieceIndex..lastPieceIndex) { it < pieces.size() && pieces.getBit(it) }?.let { Log.d(TAG, it) }
                        if (pieces.size() > lastPieceIndex)
                            _pieceMap.value = PieceMapSnapshot.capture(firstPieceIndex, lastPieceIndex,
                                havePiece = pieces::getBit)
                        val verifiedBytes = VerifiedDownload.bytes(maxFileSize, startOffset,
                            selectedPieceLength) { it < pieces.size() && pieces.getBit(it) }
                        val progress = (verifiedBytes.toDouble() * 100 / maxFileSize).toFloat()
                        val speed = status.downloadPayloadRate().toLong()
                        val peers = status.numPeers()
                        if (run != activeRun) return@Thread
                        if (subtitles.refresh(handle))
                            runCatching { cache.write("subtitles", subtitles.encodeCompleted()) }

                        downloadProgress = progress
                        val snapshot = DownloadSnapshot(maxFileSize, verifiedBytes, candidate.path, _pieceMap.value)
                        _activeDownload.value = snapshot
                        _metadataDetails.value = _metadataDetails.value.copy(downloadedBytes = verifiedBytes, videoSize = maxFileSize)
                        downloadCheckpoint?.latestProgress = snapshot
                        if (System.nanoTime() - lastProgressSave >= 5_000_000_000L) {
                            downloadCheckpoint?.saveProgress(downloadCheckpoint.latestProgress)
                            lastProgressSave = System.nanoTime()
                        }
                        downloadSpeedBytes = speed
                        numPeers = peers
                        _metadataDetails.value = _metadataDetails.value.connected(peers)
                        if (System.nanoTime() - lastCheckpoint >= 15_000_000_000L) {
                            checkpoint?.save()
                            lastCheckpoint = System.nanoTime()
                        }

                        onProgress(progress, speed)

                        val finishing = status.isFinished || subtitles.hasPending
                        if (finishing && (firstPieceIndex..lastPieceIndex).all(handle::havePiece)) {
                            checkpoint?.save(1500)
                            // A finished download must not stay announced as a seed; the paused handle still answers havePiece.
                            runCatching { handle.pause() }.onFailure { Log.w(TAG, "Unable to pause finished torrent: ${EngineFailureLog.describe(it)}") }
                            runCatching { cache.recordComplete(candidate, maxFileSize) }
                            if (cache.isComplete(candidate, maxFileSize)) localVideo = candidate
                            downloadProgress = 100f
                            val completeMap = PieceMapSnapshot.capture(firstPieceIndex, lastPieceIndex) { true }
                            _activeDownload.value = DownloadSnapshot(maxFileSize, maxFileSize, candidate.path, completeMap)
                            runCatching { cache.write("progress", _activeDownload.value!!.encode()) }
                            _metadataDetails.value = _metadataDetails.value.copy(phase = "다운로드 완료")
                            downloadSpeedBytes = 0L
                            _pieceMap.value = PieceMapSnapshot.capture(firstPieceIndex, lastPieceIndex) { true }
                            onProgress(100f, 0L)
                            break
                        }
                    } catch (t: Throwable) {
                        Log.e(TAG, "Error in progress loop: ${EngineFailureLog.describe(t)}")
                    }

                    Thread.sleep(1000)
                }

            } catch (t: Throwable) {
                // 예외 원문은 로그에도 사용자 문구에도 넣지 않는다(EngineFailureLog).
                Log.e(TAG, "Unhandled error in torrent streaming thread: " +
                    EngineFailureLog.describe(t, BuildConfig.DEBUG))
                if (run == activeRun) onError(TorrentFailure.ENGINE.text(context))
            }
        }.also { it.start() }
    }

    private fun applyDownloadOrder(handle: TorrentHandle, playbackFocused: Boolean) {
        if (DownloadOrderPolicy.order(playbackFocused).sequential) handle.setFlags(TorrentFlags.SEQUENTIAL_DOWNLOAD)
        else handle.unsetFlags(TorrentFlags.SEQUENTIAL_DOWNLOAD)
    }
    /** Index checks outside the download loop: file or libtorrent errors are logged, never thrown to the caller. */
    private fun refreshStartupIndex() = runCatching { updateStartupIndex() }
        .onFailure { Log.w(TAG, "Startup index check failed: ${EngineFailureLog.describe(it)}") }
    /** Under the engine lock, so a concurrent [updateStartupIndex] cannot bring the tail back. */
    @Synchronized private fun clearPrefetch() { prefetchPieces = emptySet() }
    @Synchronized private fun updateStartupIndex() {
        val handle = currentHandle ?: return
        val index = startupIndex ?: return
        val added = index.update(handle::havePiece) - startupPieces
        prefetchPieces = StartupPrefetchPlan.pieces(prefetchPieces, index.location(StartupBufferPlan.HEAD_BYTES))
        if (added.isEmpty()) return
        startupPieces = startupPieces + added
        added.forEach { handle.piecePriority(it, Priority.TOP_PRIORITY); handle.setPieceDeadline(it, 1000) }
    }
    @Synchronized private fun advanceDownloadWindow() {
        if (priorityWindow.isEmpty()) return
        val pieceLength = currentHandle?.torrentFile()?.pieceLength()?.toLong() ?: return
        prioritizeOffset((priorityWindow.first * pieceLength - selectedFileStartOffset).coerceAtLeast(0))
    }

    @Synchronized fun prioritizeOffset(fileByteOffset: Long) {
        if (fileByteOffset !in 0 until selectedFileLength) return
        val handle = currentHandle ?: return
        val info = handle.torrentFile() ?: return
        val pieceLength = info.pieceLength().toLong()
        if (pieceLength <= 0) return
        val absoluteTorrentOffset = selectedFileStartOffset + fileByteOffset
        val requestedPiece = (absoluteTorrentOffset / pieceLength).toInt()
            .coerceIn(firstPieceIndex, lastPieceIndex)
        val startPieceIndex = SequentialPiecePlan.nextMissing(requestedPiece, lastPieceIndex, handle::havePiece) ?: requestedPiece
        val buffer = PlaybackBuffer.of(pieceLength, PlaybackBuffer.bytesPerSecond(selectedFileLength, playbackDurationMs) ?: EngineSpec.DEFAULT_BYTES_PER_SECOND)
        val window = SequentialPiecePlan.window(startPieceIndex, lastPieceIndex, buffer)
        val offsets = readDemands.offsets().filter { it in 0 until selectedFileLength }
        val pins = startupPieces + prefetchPieces + offsets.flatMap { offset ->
            val first = ((selectedFileStartOffset + offset) / pieceLength).toInt()
            val missing = SequentialPiecePlan.nextMissing(first, lastPieceIndex, handle::havePiece) ?: first
            SequentialPiecePlan.window(missing, lastPieceIndex, buffer).toList()
        }
        val background = (offsets.ifEmpty { listOf(playbackByteOffset) }).minOf { position ->
            PlaybackDownloadPolicy.backgroundPriority(position, selectedFileLength,
                selectedFileStartOffset, pieceLength, handle::havePiece)
        }
        if (window == priorityWindow && background == backgroundPiecePriority && pins == priorityPins) return
        try {
            if (background != backgroundPiecePriority) {
                for (p in firstPieceIndex..lastPieceIndex)
                    handle.piecePriority(p, Priority.fromSwig(StartupBufferPlan.priority(p, window, pins, background)))
                backgroundPiecePriority = background
            }
            for (p in priorityWindow) if (p !in window) {
                if (p !in pins) handle.resetPieceDeadline(p)
                handle.piecePriority(p, Priority.fromSwig(StartupBufferPlan.priority(p, window, pins, background)))
            }
            priorityWindow = window
            for (p in priorityPins - pins) if (p !in window) {
                handle.resetPieceDeadline(p); handle.piecePriority(p, Priority.fromSwig(background))
            }
            for (p in pins - priorityPins) {
                handle.piecePriority(p, Priority.TOP_PRIORITY)
                handle.setPieceDeadline(p, StartupPrefetchPlan.deadlineMs(p, prefetchPieces, startupPieces))
            }
            priorityPins = pins
            for (request in SequentialPiecePlan.requests(startPieceIndex, lastPieceIndex, buffer, handle::havePiece)) {
                handle.piecePriority(request.piece, Priority.fromSwig(request.priority))
                handle.setPieceDeadline(request.piece, request.deadlineMs)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error prioritizing pieces starting at $startPieceIndex: ${EngineFailureLog.describe(e)}")
        }
    }

    fun isByteDownloaded(fileByteOffset: Long): Boolean {
        if (fileByteOffset < 0 || fileByteOffset >= selectedFileLength) return false
        val handle = currentHandle ?: return false
        val info = handle.torrentFile() ?: return false
        val pieceLength = info.pieceLength().toLong()
        if (pieceLength <= 0) return false
        val absoluteTorrentOffset = selectedFileStartOffset + fileByteOffset
        val pieceIndex = (absoluteTorrentOffset / pieceLength).toInt().coerceIn(0, info.numPieces() - 1)
        return try {
            handle.havePiece(pieceIndex)
        } catch (e: Exception) {
            false
        }
    }

    @Synchronized internal fun prioritizeRequest(offset: Long, generation: Long, exactRange: Boolean = false, readId: Long? = null) {
        if (!seekRequests.isPriorityCurrent(generation)) return
        if (readId != null) readDemands.update(readId, offset)
        if (seekRequests.isCurrent(generation) && (exactRange || !isByteDownloaded(offset))) prioritizeOffset(offset)
    }

    @Synchronized internal fun openRead(offset: Long, generation: Long): Long =
        if (seekRequests.isPriorityCurrent(generation)) readDemands.open(offset) else -1
    internal fun closeRead(id: Long) { readDemands.close(id) }

    /** The player's video length; sizes the urgent window from the real bitrate. */
    @Synchronized internal fun updatePlaybackDuration(durationMs: Long) {
        StrictMode.noteSlowCall("TorrentEngine.updatePlaybackDuration")
        playbackDurationMs = durationMs
    }
    @Synchronized fun beginSeek(targetMs: Long = 0, durationMs: Long = 0) {
        StrictMode.noteSlowCall("TorrentEngine.beginSeek")
        seekRequests.beginSeek()
        readDemands.clear()
        runCatching {
            currentHandle?.clearPieceDeadlines()
            (priorityWindow.toSet() + priorityPins).forEach { currentHandle?.piecePriority(it, Priority.LOW) }
        }
        priorityWindow = IntRange.EMPTY
        if (localVideo == null) SeekDownloadHint.offset(targetMs, durationMs, selectedFileLength)?.let {
            priorityPins = emptySet()
            playbackByteOffset = it
            runCatching { prioritizeOffset(it) }.onFailure { error -> Log.w(TAG, "Seek prefetch failed: ${EngineFailureLog.describe(error)}") }
        }
    }

    fun isPlaybackReady(): Boolean = playbackReady().also { StrictMode.noteSlowCall("TorrentEngine.isPlaybackReady") }
    @Synchronized internal fun requestPlayback(): Boolean {
        StrictMode.noteSlowCall("TorrentEngine.requestPlayback")
        if (localVideo != null) return true
        if (prioritiesSetRun != activeRun) return false
        val info = currentHandle?.torrentFile() ?: return false
        if (BuildConfig.DEBUG) startTrace?.let { Log.d(TAG, it.playbackRequested()) }
        currentHandle?.let { applyDownloadOrder(it, playbackFocused = true) }
        startupPieces = StartupBufferPlan.required(selectedFileLength, selectedFileStartOffset,
            info.pieceLength().toLong(), selectedVideoFile?.extension.orEmpty())
        prefetchPieces = StartupPrefetchPlan.tailPieces(selectedFileLength, selectedFileStartOffset,
            info.pieceLength().toLong(), selectedVideoFile?.extension.orEmpty())
        startupIndex = StartupIndexState(selectedVideoFile!!, selectedFileLength, selectedFileStartOffset,
            info.pieceLength().toLong(), selectedVideoFile!!.extension)
        refreshStartupIndex()
        beginSeek(0, 1)
        return true
    }
    @Synchronized internal fun releasePlaybackPriority() {
        StrictMode.noteSlowCall("TorrentEngine.releasePlaybackPriority")
        seekRequests.beginSeek()
        startupPieces = emptySet(); startupIndex = null; prefetchPieces = emptySet()
        readDemands.clear()
        runCatching { currentHandle?.clearPieceDeadlines()
            for (piece in firstPieceIndex..lastPieceIndex) currentHandle?.piecePriority(piece, Priority.DEFAULT)
            currentHandle?.let { handle -> applyDownloadOrder(handle, playbackFocused = false)
                DownloadOrderPolicy.edgePieces(firstPieceIndex, lastPieceIndex).forEach { handle.piecePriority(it, Priority.TOP_PRIORITY) } } }
        priorityWindow = IntRange.EMPTY; priorityPins = emptySet(); backgroundPiecePriority = -1
    }
    private fun playbackReady(): Boolean {
        if (localVideo != null) return true
        val handle = currentHandle ?: return false
        if (firstPieceIndex < 0 || lastPieceIndex < 0) return false
        // Polled every 250 ms: finding the index here, not in the 1 s loop, saves up to a second per step.
        if (startupIndex?.ready == false) refreshStartupIndex()
        if (startupIndex?.ready == false) return false
        return try {
            val required = startupPieces
            if (required.isNotEmpty()) {
                val ready = StartupBufferPlan.ready(required, handle::havePiece)
                if (ready) clearPrefetch()
                if (ready) { startupPieces = emptySet(); if (BuildConfig.DEBUG) Log.d(TAG, "Initial verified playback buffer ready") }
                if (ready && BuildConfig.DEBUG) startTrace?.ready()?.let { Log.d(TAG, it) }
                return ready
            }
            val info = handle.torrentFile() ?: return false
            TorrentPlayback.startupReady(selectedFileLength, selectedFileStartOffset,
                info.pieceLength().toLong(), handle::havePiece)
        } catch (e: Exception) {
            false
        }
    }

    private fun getFilePieceRange(torrentInfo: TorrentInfo, fileIndex: Int): Pair<Int, Int> {
        val pieceLength = torrentInfo.pieceLength().toLong()
        val fileStorage = torrentInfo.files()

        var offset = 0L
        for (i in 0 until fileIndex) {
            offset += fileStorage.fileSize(i)
        }

        val startPiece = (offset / pieceLength).toInt()
        val endPiece = ((offset + fileStorage.fileSize(fileIndex) - 1) / pieceLength).toInt()

        return Pair(startPiece, endPiece)
    }

    fun isByteAvailable(requestedByte: Long): Boolean {
        val file = selectedVideoFile ?: return false
        return file.exists() && file.length() > requestedByte
    }

    fun readablePieceBytes(position: Long, requested: Int): Int {
        val info = currentHandle?.torrentFile() ?: return 0
        return TorrentPlayback.readSize(position, requested, selectedFileLength,
            selectedFileStartOffset, info.pieceLength().toLong())
    }
    fun isStreamingFile(file: File): Boolean = currentHandle != null && selectedVideoFile == file
    fun storedFiles(): List<StoredTorrentFile> = diskStore.list()
    fun resolveInput(input: String): String {
        val resolved = TorrentInputResolver({ bytes ->
            val params = org.libtorrent4j.AddTorrentParams()
            params.setTorrentInfo(TorrentInfo.bdecode(bytes))
            org.libtorrent4j.swig.libtorrent.make_magnet_uri(params.swig())
        }).resolve(input)
        resolved.metadata?.let { bytes ->
            TorrentCache(stateRoot, MagnetIdentity.key(resolved.magnet),
                TorrentCacheCipher::encrypt, TorrentCacheCipher::decrypt).write("metadata", bytes)
        }
        return resolved.magnet
    }
    internal fun savedMagnet(entry: SavedMagnet): SavedMagnet = SavedDownloadLookup(downloadRoot) { key ->
        TorrentCache(stateRoot, key, TorrentCacheCipher::encrypt, TorrentCacheCipher::decrypt)
    }.enrich(entry)

    internal fun deleteTorrentFiles(magnet: String, protectedFiles: List<File>) {
        val key = MagnetIdentity.key(magnet)
        val cache = TorrentCache(stateRoot, key, TorrentCacheCipher::encrypt, TorrentCacheCipher::decrypt)
        val legacy = cache.read("metadata")?.let { bytes -> TorrentInfo.bdecode(bytes).files().let { files ->
            (0 until files.numFiles()).map { files.filePath(it) } } }.orEmpty()
        val remembered = cache.read("progress")?.let { runCatching { DownloadSnapshot.decode(it).path }.getOrNull() }
            ?.let { File(it).canonicalFile }?.takeIf { it.toPath().startsWith(downloadRoot.canonicalFile.toPath()) &&
                !it.toPath().startsWith(File(downloadRoot, key).canonicalFile.toPath()) }
            ?.relativeTo(downloadRoot.canonicalFile)?.invariantSeparatorsPath
        TorrentRemoval(downloadRoot, stateRoot).delete(key, legacy + listOfNotNull(remembered), protectedFiles)
    }
    internal fun awaitStoppedWrites() {
        check(teardown.await()) { "다운로드 종료를 기다리는 중입니다. 잠시 후 다시 삭제해 주세요." }
        retiredWorkers.toList().forEach { it.join(5000); check(!it.isAlive) { "다운로드 종료를 기다리는 중입니다. 잠시 후 다시 삭제해 주세요." }; retiredWorkers.remove(it) }
        closingCheckpoints.toList().forEach { it.awaitClosed(); closingCheckpoints.remove(it) }
    }
    @Synchronized fun deleteStoredFile(path: String): Boolean {
        check(currentHandle == null && localVideo == null && worker?.isAlive != true) {
            "Stop streaming before deleting stored files"
        }
        check(teardown.await()) { "다운로드 종료를 기다리는 중입니다. 잠시 후 다시 삭제해 주세요." }
        return diskStore.delete(path)
    }

    @Synchronized
    fun stop() {
        val completed = _activeDownload.value?.takeIf { it.downloaded == it.size && it.path == selectedVideoFile?.path }
        val finalCache = activeCache
        activeCache = null
        activeRun++
        _pieceMap.value = PieceMapSnapshot.EMPTY
        _metadataDetails.value = MetadataDetails()
        _activeDownload.value = null
        subtitles.clear()
        priorityWindow = IntRange.EMPTY
        backgroundPiecePriority = -1
        playbackByteOffset = 0L
        startupPieces = emptySet()
        startupIndex = null
        readDemands.clear()
        priorityPins = emptySet()
        startTrace = null
        prefetchPieces = emptySet()
        worker?.interrupt()
        retiredWorkers.removeAll { !it.isAlive }
        worker?.takeIf { it.isAlive }?.let { retiredWorkers.add(it) }
        worker = null
        val closing = checkpoint
        checkpoint = null
        closingCheckpoints.removeAll { it.isClosed }
        closing?.let { closingCheckpoints.add(it) }
        localVideo = null
        val session = sessionManager
        val handle = currentHandle
        currentHandle = null
        sessionManager = null
        selectedVideoFile = null
        selectedFileLength = 0L
        downloadProgress = 0f
        downloadSpeedBytes = 0L
        numPeers = 0
        // Saving resume data and stopping the native session can block for seconds; never do it on the caller thread.
        teardown.submit {
            runCatching { closing?.save(EngineSpec.RESUME_SAVE_WAIT_MS) }
            closing?.close()
            handle?.let { runCatching { session?.remove(it) } }
            sharedSessions.release(this)
            if (completed != null) runCatching { finalCache?.recordComplete(File(completed.path), completed.size) }
            ActiveTorrentFiles.release(this)
        }
    }
}
