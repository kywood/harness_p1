package com.example.torrentstreamplayer.engine

import java.io.File
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class TorrentCacheTest {
    @Test fun savedProgressIsEncryptedAndSharedWithHistoryAfterRestart() {
        val video = folder.newFile("video.mp4").apply { writeBytes(ByteArray(100)) }
        val snapshot = DownloadSnapshot(100, 60, video.path)
        cache().write("progress", snapshot.encode())
        val entry = SavedMagnet("magnet:?xt=urn:btih:" + "1".repeat(40), "Movie", 1)
        val loaded = SavedDownloadLookup(folder.root) { cache() }.enrich(entry)
        assertEquals(100L, loaded.fileSize); assertEquals(60L, loaded.downloadedBytes)
        assertFalse(String(File(folder.root, "$id/progress.bin").readBytes()).contains(video.path))
    }
    @get:Rule val folder = TemporaryFolder()
    private val id = "a".repeat(64)
    private val key = SecretKeySpec(ByteArray(16) { it.toByte() }, "AES")
    private fun encrypt(bytes: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        return cipher.iv + cipher.doFinal(bytes)
    }
    private fun decrypt(bytes: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
        return cipher.doFinal(bytes, 12, bytes.size - 12)
    }
    private fun cache() = TorrentCache(folder.root, id, ::encrypt, ::decrypt)
    @Test fun metadataAndResumeSurviveRestartWithoutPlaintext() {
        val metadata = "private torrent metadata".toByteArray()
        val resume = byteArrayOf(4, 8, 15, 16, 23, 42)
        cache().write("metadata", metadata)
        cache().write("resume", resume)
        assertArrayEquals(metadata, cache().read("metadata"))
        assertArrayEquals(resume, cache().read("resume"))
        assertFalse(File(folder.root, id + "/metadata.bin").readText().contains("private torrent metadata"))
    }
    @Test fun preallocatedPartialFileCannotBeMistakenForCompletedVideo() {
        val file = folder.newFile("partial.mp4").apply { writeBytes(ByteArray(4096)) }
        assertFalse(cache().isComplete(file, 4096))
    }
    @Test fun completedVideoIsReusableAfterRestart() {
        val file = folder.newFile("video.mp4").apply { writeBytes(ByteArray(32) { it.toByte() }) }
        cache().recordComplete(file, 32)
        assertTrue(cache().isComplete(file, 32))
        assertFalse(cache().isComplete(file, 31))
        assertFalse(cache().isComplete(folder.newFile("different.mp4"), 32))
    }
    @Test fun deletedOrModifiedCompletedFilesRequireVerificationAgain() {
        val file = folder.newFile("video.mp4").apply { writeBytes(ByteArray(32)) }
        cache().recordComplete(file, 32)
        assertTrue(file.setLastModified(file.lastModified() + 10_000))
        assertFalse(cache().isComplete(file, 32))
        cache().recordComplete(file, 32)
        assertTrue(file.delete())
        assertFalse(cache().isComplete(file, 32))
    }
    @Test fun corruptedOrMissingEncryptedStateFallsBackSafely() {
        assertNull(cache().read("resume"))
        cache().write("resume", byteArrayOf(1, 2, 3))
        val file = File(folder.root, id + "/resume.bin")
        val bytes = file.readBytes().also { it[it.lastIndex] = (it.last().toInt() xor 1).toByte() }
        file.writeBytes(bytes)
        assertNull(cache().read("resume"))
    }
    @Test fun strictReadTellsAMissingFileFromAFailedRead() {
        assertNull("no file is not a failure", cache().readOrThrow("links"))
        cache().write("links", byteArrayOf(1, 2, 3))
        val failing = TorrentCache(folder.root, id, ::encrypt) { throw AEADBadTagException() }
        assertThrows(AEADBadTagException::class.java) { failing.readOrThrow("links") }
        assertNull("the lenient read still hides the failure", failing.read("links"))
        assertArrayEquals(byteArrayOf(1, 2, 3), cache().readOrThrow("links"))
    }
    @Test fun failedWriteKeepsPreviousCheckpoint() {
        cache().write("resume", byteArrayOf(7))
        val broken = TorrentCache(folder.root, id, { error("Encryption failed") }, ::decrypt)
        assertThrows(IllegalStateException::class.java) { broken.write("resume", byteArrayOf(8)) }
        assertArrayEquals(byteArrayOf(7), cache().read("resume"))
        assertFalse(File(folder.root, id + "/.resume.tmp").exists())
    }
    @Test fun rejectsPathsOutsideCacheNamespace() {
        assertThrows(IllegalArgumentException::class.java) {
            TorrentCache(folder.root, "../escape", ::encrypt, ::decrypt)
        }
        assertThrows(IllegalArgumentException::class.java) { cache().write("../escape", byteArrayOf()) }
    }
}
