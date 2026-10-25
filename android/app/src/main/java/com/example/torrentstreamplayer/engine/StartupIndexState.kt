package com.example.torrentstreamplayer.engine

import java.io.File
import java.io.RandomAccessFile

internal class StartupIndexState(private val file: File, private val size: Long,
    private val offset: Long, private val pieceSize: Long, extension: String) {
    @Volatile var ready = extension.lowercase(java.util.Locale.ROOT) !in setOf("mp4", "mov", "m4v")
        private set
    private var found: Mp4IndexProbe.Found? = null
    private fun pieces(start: Long, length: Long): Set<Int> =
        ((offset + start) / pieceSize).toInt().rangeTo(((offset + start + length - 1) / pieceSize).toInt()).toSet()
    fun update(havePiece: (Int) -> Boolean): Set<Int> {
        if (ready) return emptySet()
        val result = found ?: Mp4IndexProbe.inspect(size) { start, length ->
            if (!pieces(start, length.toLong()).all(havePiece)) null
            else RandomAccessFile(file, "r").use { input ->
                input.seek(start); ByteArray(length).also { input.readFully(it) }
            }
        }
        return when (result) {
            is Mp4IndexProbe.Need -> pieces(result.offset, result.length.toLong())
            is Mp4IndexProbe.Found -> {
                found = result
                pieces(result.offset, result.length).also { ready = it.all(havePiece) }
            }
            Mp4IndexProbe.Unknown -> { ready = true; emptySet() }
        }
    }
}
