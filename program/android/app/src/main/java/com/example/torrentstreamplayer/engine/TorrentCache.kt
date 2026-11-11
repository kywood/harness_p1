package com.example.torrentstreamplayer.engine

import java.io.*
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.AtomicMoveNotSupportedException

internal class TorrentCache(
    root: File, key: String,
    private val encrypt: (ByteArray) -> ByteArray,
    private val decrypt: (ByteArray) -> ByteArray
) {
    private val directory = File(root, key).also { require(key.matches(Regex("[0-9a-f]{64}"))) }
    private fun path(kind: String): File {
        require(kind.matches(Regex("[a-z]+")))
        return File(directory, kind + ".bin")
    }
    @Synchronized fun read(kind: String): ByteArray? = try {
        val file = path(kind)
        if (!file.isFile || file.length() > 16L * 1024 * 1024) null else decrypt(file.readBytes())
    } catch (_: Exception) { null }
    @Synchronized fun write(kind: String, data: ByteArray) {
        require(data.size <= 16 * 1024 * 1024)
        check(directory.isDirectory || directory.mkdirs())
        val target = path(kind)
        val temporary = File(directory, "." + kind + ".tmp")
        try {
            FileOutputStream(temporary).use { it.write(encrypt(data)); it.fd.sync() }
            try {
                Files.move(temporary.toPath(), target.toPath(),
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        } finally { temporary.delete() }
    }
    fun recordComplete(file: File, expectedSize: Long) {
        require(file.isFile && expectedSize > 0 && file.length() == expectedSize)
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use {
            it.writeUTF(file.canonicalPath)
            it.writeLong(expectedSize)
            it.writeLong(file.lastModified())
        }
        write("complete", bytes.toByteArray())
    }
    fun isComplete(file: File, expectedSize: Long): Boolean = try {
        val bytes = read("complete")
        bytes != null && DataInputStream(ByteArrayInputStream(bytes)).use {
            it.readUTF() == file.canonicalPath && it.readLong() == expectedSize &&
                it.readLong() == file.lastModified() && file.isFile &&
                expectedSize > 0 && file.length() == expectedSize
        }
    } catch (_: Exception) { false }
}
