package com.example.torrentstreamplayer.engine

internal object TorrentPlayback {
    private val videoExtensions = setOf("mp4", "mkv", "avi", "webm", "mov", "m4v", "ts", "flv")
    fun isVideo(path: String, size: Long): Boolean =
        size > 0 && path.substringAfterLast('.', "").lowercase(java.util.Locale.ROOT) in videoExtensions
    fun readSize(position: Long, requested: Int, size: Long, offset: Long, pieceSize: Long): Int {
        require(position >= 0 && requested >= 0 && size >= 0 && offset >= 0 && pieceSize > 0)
        if (position >= size) return 0
        val inPiece = (offset % pieceSize + position % pieceSize) % pieceSize
        return minOf(requested.toLong(), size - position, pieceSize - inPiece).toInt()
    }
    fun startupReady(size: Long, offset: Long, pieceSize: Long, havePiece: (Int) -> Boolean): Boolean {
        if (size <= 0 || offset < 0 || pieceSize <= 0) return false
        val first = offset / pieceSize
        val last = (offset + minOf(size, 1024L * 1024) - 1) / pieceSize
        return (first..last).all { havePiece(it.toInt()) }
    }
    fun httpRange(header: String?, size: Long): LongRange? {
        if (size <= 0) return null
        if (header == null) return 0L..(size - 1)
        val match = Regex("bytes=(\\d*)-(\\d*)").matchEntire(header.trim()) ?: return null
        val (start, end) = match.destructured
        return parseRange(start, end, size)
    }
    private fun parseRange(start: String, end: String, size: Long): LongRange? {
        if (start.isEmpty()) {
            val suffix = end.toLongOrNull()?.takeIf { it > 0 } ?: return null
            return (size - minOf(size, suffix))..(size - 1)
        }
        val first = start.toLongOrNull()?.takeIf { it < size } ?: return null
        val last = if (end.isEmpty()) size - 1 else end.toLongOrNull() ?: return null
        return if (last < first) null else first..minOf(last, size - 1)
    }
}
