package com.example.torrentstreamplayer.engine

import com.example.torrentstreamplayer.meta.SubtitleSpec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.libtorrent4j.Priority
import org.libtorrent4j.TorrentHandle
import org.libtorrent4j.TorrentInfo
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Subtitle files of one engine run: which ones to download ([SubtitleFilePlan]), urging their pieces ahead of the
 * video, and publishing the verified ones as [files]. Each subtitle path is checked to stay inside the download
 * folder. Nothing here uploads: the session's download-only settings and the pause on completion still apply.
 *
 * The engine calls it from its worker thread and from `stop()`, so every method is synchronized.
 */
internal class SubtitleDownloads {
    private var planned: List<Pair<SubtitleCandidate, File>> = emptyList()
    private val completed = linkedSetOf<Int>()
    private val mutableFiles = MutableStateFlow<List<SubtitleFile>>(emptyList())
    val files: StateFlow<List<SubtitleFile>> = mutableFiles.asStateFlow()

    /** True while a chosen subtitle is still missing pieces. */
    val hasPending: Boolean @Synchronized get() = planned.any { it.first.index !in completed }

    /** File indexes to download besides the video. */
    val fileIndexes: Set<Int> @Synchronized get() = planned.map { it.first.index }.toSet()

    /** Chooses the subtitles of [videoIndex] from [info]. Paths escaping [downloadDir] are dropped. */
    fun select(info: TorrentInfo, videoIndex: Int, downloadDir: File) {
        val storage = info.files()
        var offset = 0L
        val entries = (0 until storage.numFiles()).map { index ->
            TorrentFileEntry(index, storage.filePath(index), storage.fileSize(index), offset)
                .also { offset += it.size }
        }
        select(entries, videoIndex, info.pieceLength().toLong(), downloadDir)
    }

    @Synchronized fun select(entries: List<TorrentFileEntry>, videoIndex: Int, pieceLength: Long, downloadDir: File) {
        val root = downloadDir.canonicalFile.toPath()
        planned = SubtitleFilePlan.select(entries, videoIndex, pieceLength).mapNotNull { candidate ->
            File(downloadDir, candidate.path).canonicalFile.takeIf { it.toPath().startsWith(root) }
                ?.let { candidate to it }
        }
        completed.clear()
        publish()
    }

    /** Asks libtorrent for the chosen files at top priority, after the engine set every other file to IGNORE. */
    fun prioritize(handle: TorrentHandle) {
        fileIndexes.forEach { handle.filePriority(it, Priority.TOP_PRIORITY) }
        urge(handle)
    }

    /**
     * Records subtitles whose pieces all passed the hash check and re-urges the missing pieces (a seek clears every
     * piece deadline). Returns true when a new subtitle finished, so the caller can save [encodeCompleted].
     */
    fun refresh(handle: TorrentHandle): Boolean {
        val finished = track(handle::havePiece)
        if (hasPending) urge(handle)
        return finished
    }

    /** Pure part of [refresh]: marks complete files. Returns true when one finished in this call. */
    @Synchronized fun track(havePiece: (Int) -> Boolean): Boolean {
        val newly = planned.filter { (candidate, _) ->
            candidate.index !in completed && candidate.pieces.all(havePiece)
        }
        if (newly.isEmpty()) return false
        newly.forEach { completed += it.first.index }
        publish()
        return true
    }

    @Synchronized private fun urge(handle: TorrentHandle) {
        planned.filter { it.first.index !in completed }.flatMap { it.first.pieces }.distinct()
            .filterNot(handle::havePiece).forEach {
                handle.piecePriority(it, Priority.TOP_PRIORITY)
                handle.setPieceDeadline(it, SubtitleSpec.PIECE_DEADLINE_MS)
            }
    }

    /** Completed file indexes for the engine's encrypted cache. */
    @Synchronized fun encodeCompleted(): ByteArray = ByteArrayOutputStream().also { bytes ->
        DataOutputStream(bytes).use { out ->
            out.writeInt(completed.size)
            completed.forEach(out::writeInt)
        }
    }.toByteArray()

    /**
     * Restores completion saved by [encodeCompleted] for an already complete video, which never gets a handle.
     * Only files still planned and present on disk with their full size are published.
     */
    @Synchronized fun restore(saved: ByteArray?) {
        val indexes = runCatching {
            DataInputStream(ByteArrayInputStream(saved ?: return)).use { input ->
                val count = input.readInt()
                require(count in 0..planned.size)
                List(count) { input.readInt() }
            }
        }.getOrNull() ?: return
        val present = planned.filter { (candidate, file) ->
            candidate.index in indexes && file.isFile && file.length() == candidate.size
        }
        completed += present.map { it.first.index }
        publish()
    }

    @Synchronized fun clear() {
        planned = emptyList()
        completed.clear()
        publish()
    }

    private fun publish() {
        mutableFiles.value = planned.filter { it.first.index in completed }.map { (candidate, file) ->
            SubtitleFile(file, candidate.language, candidate.matchesVideoName)
        }
    }
}
