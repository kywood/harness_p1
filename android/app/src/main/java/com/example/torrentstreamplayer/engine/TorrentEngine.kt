package com.example.torrentstreamplayer.engine

import android.content.Context
import android.util.Log
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class TorrentEngine(private val context: Context) {

    private var sessionManager: SessionManager? = null
    @Volatile private var currentHandle: TorrentHandle? = null
    @Volatile private var activeRun = 0L
    @Volatile var localVideo: File? = null
        private set
    private var worker: Thread? = null
    private var checkpoint: TorrentCheckpoint? = null
    internal val seekRequests = SeekRequestTracker()
    private var priorityWindow = IntRange.EMPTY
    private val _pieceMap = MutableStateFlow(PieceMapSnapshot.EMPTY)
    val pieceMap = _pieceMap.asStateFlow()
    private val downloadRoot = File(context.cacheDir, "torrent_downloads")
    private val stateRoot = File(context.cacheDir, "torrent_state")
    private val diskStore = TorrentDiskStore(listOf(downloadRoot, stateRoot))
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
    }

    @Synchronized
    private fun ensureStarted(run: Long): SessionManager {
        check(run == activeRun) { "Stream was cancelled" }
        val existing = sessionManager
        if (existing != null && existing.isRunning) {
            return existing
        }
        val newSession = SessionManager()
        val settings = SettingsPack()
        settings.setInteger(settings_pack.int_types.initial_picker_threshold.swigValue(), 0)
        DownloadOnlyPolicy.settings(settings_pack.int_types.choking_algorithm,
            settings_pack.int_types.unchoke_slots_limit, settings_pack.int_types.allowed_fast_set_size,
            settings_pack.choking_algorithm_t.fixed_slots_choker.swigValue()
        ).forEach { (key, value) -> settings.setInteger(key.swigValue(), value) }
        newSession.start(SessionParams(settings))
        try {
            val peerClasses = ip_filter()
            DownloadOnlyPolicy.assignPeerClasses { first, last, mask ->
                peerClasses.add_rule(Address.parseIp(first).swig(), Address.parseIp(last).swig(), mask)
            }
            newSession.swig().set_peer_class_filter(peerClasses)
        } catch (t: Throwable) {
            newSession.stop()
            throw t
        }
        sessionManager = newSession
        return newSession
    }

    @Synchronized fun startStreaming(
        magnetUrl: String,
        onMetadataReceived: (fileName: String, fileSize: Long) -> Unit,
        onProgress: (progress: Float, downloadSpeedBytes: Long) -> Unit,
        onError: (String) -> Unit
    ) {
        val key = try { MagnetIdentity.key(magnetUrl) }
        catch (_: Exception) { onError("Invalid magnet link or info hash"); return }
        stop()
        val run = activeRun
        val cache = TorrentCache(stateRoot, key, TorrentCacheCipher::encrypt, TorrentCacheCipher::decrypt)
        var downloadDir = File(downloadRoot, key)
        if (!downloadDir.exists()) {
            downloadDir.mkdirs()
        }

        worker = Thread {
            try {
                Log.d(TAG, "Fetching magnet metadata...")
                val (metadataBytes, session) = try {
                    MetadataStartup.load(cache.read("metadata"), { ensureStarted(run) },
                        { Thread.sleep(500) }) { it.fetchMagnet(magnetUrl, 45, downloadDir) }
                } catch (t: Throwable) {
                    Log.e(TAG, "Error in fetchMagnet", t)
                    null to null
                }

                if (run != activeRun) return@Thread
                if (metadataBytes == null || metadataBytes.isEmpty()) {
                    onError("Failed to fetch torrent metadata within timeout. Please check your network or peer availability.")
                    return@Thread
                }

                val torrentInfo = try {
                    TorrentInfo.bdecode(metadataBytes)
                } catch (t: Throwable) {
                    Log.e(TAG, "Error in TorrentInfo.bdecode", t)
                    null
                }

                if (torrentInfo == null) {
                    onError("Failed to decode torrent metadata.")
                    return@Thread
                }

                val fileStorage = torrentInfo.files()
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
                    onError("No valid video file found in torrent.")
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
                selectedFileStartOffset = startOffset
                selectedFileLength = maxFileSize
                selectedVideoFile = candidate
                val selectedRange = getFilePieceRange(torrentInfo, bestFileIndex)
                firstPieceIndex = selectedRange.first
                lastPieceIndex = selectedRange.second
                _pieceMap.value = PieceMapSnapshot.capture(firstPieceIndex, lastPieceIndex) { false }
                if (cache.isComplete(candidate, maxFileSize)) {
                    _pieceMap.value = PieceMapSnapshot.capture(firstPieceIndex, lastPieceIndex) { true }
                    localVideo = candidate
                    downloadProgress = 100f
                    onMetadataReceived(candidate.name, maxFileSize)
                    onProgress(100f, 0L)
                    return@Thread
                }

                Log.d(TAG, "Selected file: $selectedRelativePath, Size: $maxFileSize bytes, Offset in torrent: $selectedFileStartOffset")
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

                // Ascending deadlines follow playback; native sequential mode would favor the file start.
                try {
                    handle.unsetFlags(TorrentFlags.SEQUENTIAL_DOWNLOAD)
                } catch (t: Throwable) {
                    Log.e(TAG, "Error setting sequential download flag", t)
                    onError("Failed to enable sequential download.")
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

                    val filePieceRange = getFilePieceRange(torrentInfo, bestFileIndex)
                    firstPieceIndex = filePieceRange.first
                    lastPieceIndex = filePieceRange.second

                    // Prioritize first and last pieces for video header / index metadata
                    handle.piecePriority(firstPieceIndex, Priority.TOP_PRIORITY)
                    if (firstPieceIndex + 1 <= lastPieceIndex) {
                        handle.piecePriority(firstPieceIndex + 1, Priority.TOP_PRIORITY)
                    }
                    if (lastPieceIndex - 1 >= firstPieceIndex) {
                        handle.piecePriority(lastPieceIndex - 1, Priority.TOP_PRIORITY)
                    }
                    handle.piecePriority(lastPieceIndex, Priority.TOP_PRIORITY)
                } catch (t: Throwable) {
                    Log.e(TAG, "Error setting piece priorities", t)
                    onError("Failed to configure video download priorities.")
                    return@Thread
                }

                prioritizeOffset(0L)
                onMetadataReceived(selectedVideoFile?.name ?: "Video", selectedFileLength)

                // Progress polling loop
                while (run == activeRun && activeSession.isRunning && currentHandle != null) {
                    try {
                        val status = handle.status(TorrentHandle.QUERY_PIECES)
                        advanceDownloadWindow()
                        val pieces = status.pieces()
                        if (pieces.size() > lastPieceIndex)
                            _pieceMap.value = PieceMapSnapshot.capture(firstPieceIndex, lastPieceIndex,
                                havePiece = pieces::getBit)
                        val map = _pieceMap.value
                        val progress = if (map.total > 0) map.verified * 100f / map.total else 0f
                        val speed = status.downloadPayloadRate().toLong()
                        val peers = status.numPeers()

                        downloadProgress = progress
                        downloadSpeedBytes = speed
                        numPeers = peers
                        if (System.nanoTime() - lastCheckpoint >= 15_000_000_000L) {
                            checkpoint?.save()
                            lastCheckpoint = System.nanoTime()
                        }

                        onProgress(progress, speed)

                        if (status.isFinished && (firstPieceIndex..lastPieceIndex).all(handle::havePiece)) {
                            checkpoint?.save(1500)
                            runCatching { cache.recordComplete(candidate, maxFileSize) }
                            downloadProgress = 100f
                            downloadSpeedBytes = 0L
                            _pieceMap.value = PieceMapSnapshot.capture(firstPieceIndex, lastPieceIndex) { true }
                            onProgress(100f, 0L)
                            break
                        }
                    } catch (t: Throwable) {
                        Log.e(TAG, "Error in progress loop", t)
                    }

                    Thread.sleep(1000)
                }

            } catch (t: Throwable) {
                Log.e(TAG, "Unhandled error in torrent streaming thread", t)
                if (run == activeRun) onError("Torrent engine error: ${t.localizedMessage ?: "Unknown error"}")
            }
        }.also { it.start() }
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
        val startPieceIndex = SequentialPiecePlan.nextMissing(requestedPiece, lastPieceIndex, handle::havePiece) ?: return
        val window = SequentialPiecePlan.window(startPieceIndex, lastPieceIndex)
        if (window == priorityWindow) return
        try {
            for (p in firstPieceIndex..lastPieceIndex)
                handle.piecePriority(p, Priority.fromSwig(SequentialPiecePlan.priority(p, window)))
            for (p in priorityWindow) if (p !in window) {
                handle.resetPieceDeadline(p)
                handle.piecePriority(p, Priority.IGNORE)
            }
            priorityWindow = window
            for (request in SequentialPiecePlan.requests(startPieceIndex, lastPieceIndex, handle::havePiece)) {
                handle.piecePriority(request.piece, Priority.fromSwig(request.priority))
                handle.setPieceDeadline(request.piece, request.deadlineMs)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error prioritizing pieces starting at $startPieceIndex", e)
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

    @Synchronized internal fun prioritizeRequest(offset: Long, generation: Long) {
        if (seekRequests.isCurrent(generation)) prioritizeOffset(offset)
    }

    @Synchronized fun beginSeek() {
        seekRequests.beginSeek()
        runCatching {
            currentHandle?.clearPieceDeadlines()
            priorityWindow.forEach { currentHandle?.piecePriority(it, Priority.IGNORE) }
        }
        priorityWindow = IntRange.EMPTY
    }

    fun isPlaybackReady(): Boolean {
        if (localVideo != null) return true
        val handle = currentHandle ?: return false
        if (firstPieceIndex < 0 || lastPieceIndex < 0) return false
        return try {
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
    @Synchronized fun deleteStoredFile(path: String): Boolean {
        check(currentHandle == null && localVideo == null && worker?.isAlive != true) {
            "Stop streaming before deleting stored files"
        }
        return diskStore.delete(path)
    }

    @Synchronized
    fun stop() {
        activeRun++
        _pieceMap.value = PieceMapSnapshot.EMPTY
        priorityWindow = IntRange.EMPTY
        worker?.interrupt()
        worker = null
        runCatching { checkpoint?.save(1500) }
        checkpoint?.close()
        checkpoint = null
        localVideo = null
        try {
            val session = sessionManager
            currentHandle?.let {
                try {
                    session?.remove(it)
                } catch (ignored: Throwable) {
                }
            }
            currentHandle = null
            session?.stop()
            sessionManager = null
            selectedVideoFile = null
            selectedFileLength = 0L
            downloadProgress = 0f
            downloadSpeedBytes = 0L
            numPeers = 0
        } catch (t: Throwable) {
            Log.e(TAG, "Error stopping torrent engine", t)
        }
    }
}
