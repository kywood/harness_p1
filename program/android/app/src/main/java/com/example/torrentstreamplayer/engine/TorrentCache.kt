package com.example.torrentstreamplayer.engine

import java.io.*
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.AtomicMoveNotSupportedException

private val KEY = Regex("[0-9a-f]{64}")
private val KIND = Regex("[a-z]+")

internal class TorrentCache(
    root: File, key: String,
    private val encrypt: (ByteArray) -> ByteArray,
    private val decrypt: (ByteArray) -> ByteArray
) {
    private val directory = File(root, key).also { require(key.matches(KEY)) }
    private fun path(kind: String): File {
        require(kind.matches(KIND))
        return File(directory, kind + ".bin")
    }
    @Synchronized fun read(kind: String): ByteArray? = try { readOrThrow(kind) } catch (_: Exception) { null }
    /** Like [read], but null only means no file: a read or decrypt failure is thrown so the caller can keep the file. */
    @Synchronized fun readOrThrow(kind: String): ByteArray? {
        val file = path(kind)
        return if (!file.isFile || file.length() > 16L * 1024 * 1024) null else decrypt(file.readBytes())
    }
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
    /** Moves a [kind] file that can never be read to `<kind>.bak` (replacing an older one) so a new one can start. */
    @Synchronized fun keepAside(kind: String) {
        val file = path(kind)
        val aside = File(directory, kind + ".bak")
        if (file.isFile) Files.move(file.toPath(), aside.toPath(), StandardCopyOption.REPLACE_EXISTING)
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
