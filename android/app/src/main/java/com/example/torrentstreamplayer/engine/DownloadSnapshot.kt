package com.example.torrentstreamplayer.engine

import java.io.*

internal data class DownloadSnapshot(val size: Long, val downloaded: Long, val path: String,
    val pieces: PieceMapSnapshot? = null) {
    init { require(size > 0 && downloaded in 0..size) }
    fun available(): DownloadSnapshot {
        val file = File(path)
        val map = if (!file.isFile) pieces?.copy(cells = pieces.cells.map { PieceCell.MISSING }, verified = 0)
            else if (file.length() < downloaded) null else pieces
        return copy(downloaded = if (file.isFile) minOf(downloaded, file.length()) else 0, pieces = map)
    }
    fun encode(): ByteArray {
        val buffer = ByteArrayOutputStream()
        DataOutputStream(buffer).use {
            it.writeInt(2); it.writeLong(size); it.writeLong(downloaded); it.writeUTF(path)
            PieceMapCodec.write(it, pieces)
        }
        return buffer.toByteArray()
    }
    companion object {
        fun decode(bytes: ByteArray): DownloadSnapshot = DataInputStream(ByteArrayInputStream(bytes)).use {
            val version = it.readInt()
            require(version in 1..2)
            val size = it.readLong(); val downloaded = it.readLong(); val path = it.readUTF()
            DownloadSnapshot(size, downloaded, path, if (version == 2) PieceMapCodec.read(it) else null)
        }
    }
}
