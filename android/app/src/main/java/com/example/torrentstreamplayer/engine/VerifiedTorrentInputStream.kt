package com.example.torrentstreamplayer.engine

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.RandomAccessFile

internal class VerifiedTorrentInputStream(
    private val file: File, private var position: Long, private val endExclusive: Long,
    private val isReady: (Long) -> Boolean, private val request: (Long) -> Unit,
    private val isActive: () -> Boolean, private val readSize: (Long, Int) -> Int,
    private val timeoutMs: Long = 60_000,
    private val prefetch: (Long) -> Unit = {},
    private val canWait: () -> Boolean = { true }, private val onClosed: () -> Unit = {}
) : InputStream() {
    @Volatile private var closed = false
    private var reader: RandomAccessFile? = null
    override fun read(): Int {
        val buffer = ByteArray(1)
        return if (read(buffer, 0, 1) < 0) -1 else buffer[0].toInt() and 255
    }
    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        require(offset >= 0 && length >= 0 && offset <= buffer.size - length)
        if (length == 0) return 0
        if (closed || !isActive()) throw IOException("Torrent stream stopped")
        if (position >= endExclusive) return -1
        prefetch(position)
        awaitPiece()
        return readVerified(buffer, offset, length)
    }
    private fun awaitPiece() {
        val started = System.nanoTime()
        var nextRequestAt = started
        while (!isReady(position)) {
            if (!canWait()) throw IOException("Torrent range superseded")
            if (closed || !isActive()) throw IOException("Torrent stream stopped")
            val now = System.nanoTime()
            if (now >= nextRequestAt) {
                request(position)
                nextRequestAt = now + 250_000_000L
            }
            if ((System.nanoTime() - started) / 1_000_000 >= timeoutMs)
                throw IOException("Timed out waiting for downloaded piece")
            pause()
        }
    }
    private fun pause() {
        try {
            Thread.sleep(50)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw java.io.InterruptedIOException("Torrent read interrupted")
        }
    }
    @Synchronized private fun readVerified(buffer: ByteArray, offset: Int, length: Int): Int {
        if (closed || !isActive()) throw IOException("Torrent stream stopped")
        val count = readSize(position, minOf(length.toLong(), endExclusive - position).toInt())
        if (count <= 0) throw IOException("Invalid torrent read boundary")
        val input = reader ?: RandomAccessFile(file, "r").also { reader = it }
        input.seek(position)
        val result = input.read(buffer, offset, count)
        if (result < 0) throw IOException("Verified torrent data missing on disk")
        position += result
        return result
    }
    @Synchronized override fun close() {
        if (closed) return
        closed = true
        onClosed()
        reader?.close()
        reader = null
    }
}
