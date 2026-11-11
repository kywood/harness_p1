package com.example.torrentstreamplayer.engine

import java.io.File
import org.libtorrent4j.TorrentInfo
import org.libtorrent4j.Vectors
import org.libtorrent4j.swig.*

internal class SavedDownloadLookup(private val root: File, private val cache: (String) -> TorrentCache) {
    fun enrich(entry: SavedMagnet): SavedMagnet = runCatching {
        val storage = cache(MagnetIdentity.key(entry.magnet))
        val saved = storage.read("progress")?.let { runCatching { DownloadSnapshot.decode(it) }.getOrNull() }
        if (saved != null && File(saved.path).canonicalFile.toPath().startsWith(root.canonicalFile.toPath())) {
            val available = saved.available()
            return entry.copy(fileSize = available.size, downloadedBytes = available.downloaded, pieces = available.pieces)
        }
        val metadata = storage.read("metadata") ?: return entry
        val info = TorrentInfo.bdecode(metadata)
        val files = info.files()
        val index = (0 until files.numFiles()).filter { TorrentPlayback.isVideo(files.filePath(it), files.fileSize(it)) }
            .maxByOrNull { files.fileSize(it) } ?: return entry
        val size = files.fileSize(index)
        val offset = (0 until index).sumOf { files.fileSize(it) }
        val key = MagnetIdentity.key(entry.magnet)
        val current = File(File(root, key), files.filePath(index)).canonicalFile
        val legacy = File(root, files.filePath(index)).canonicalFile
        require(current.toPath().startsWith(root.canonicalFile.toPath()))
        require(legacy.toPath().startsWith(root.canonicalFile.toPath()))
        val file = if (current.isFile) current else legacy
        val pieceSize = info.pieceLength().toLong()
        val first = (offset / pieceSize).toInt()
        val last = ((offset + size - 1) / pieceSize).toInt()
        var pieceMap: PieceMapSnapshot? = null
        val downloaded = when {
            !file.isFile -> { pieceMap = PieceMapSnapshot.capture(first, last) { false }; 0L }
            storage.isComplete(file, size) -> { pieceMap = PieceMapSnapshot.capture(first, last) { true }; size }
            else -> storage.read("resume")?.let { bytes ->
                val error = error_code()
                val params = libtorrent.read_resume_data_ex(Vectors.bytes2byte_vector(bytes), error)
                if (error.value() != 0) null else {
                    val bits = params.get_have_pieces()
                    pieceMap = PieceMapSnapshot.capture(first, last) { it < bits.size() && bits.get_bit(it) }
                    VerifiedDownload.bytes(size, offset, info.pieceLength().toLong()) {
                        it < bits.size() && bits.get_bit(it)
                    }
                }
            }
        }
        entry.copy(fileSize = size, downloadedBytes = downloaded?.coerceAtMost(file.length().coerceAtLeast(0)), pieces = pieceMap)
    }.getOrDefault(entry)
}
