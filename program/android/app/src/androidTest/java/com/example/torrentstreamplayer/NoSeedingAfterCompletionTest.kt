package com.example.torrentstreamplayer

import android.app.Application
import androidx.test.platform.app.InstrumentationRegistry
import com.example.torrentstreamplayer.engine.MagnetIdentity
import com.example.torrentstreamplayer.engine.TorrentCache
import com.example.torrentstreamplayer.engine.TorrentCacheCipher
import com.example.torrentstreamplayer.engine.TorrentEngine
import com.example.torrentstreamplayer.engine.TorrentStorage
import com.example.torrentstreamplayer.meta.TorrentStorageDir
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.libtorrent4j.AddTorrentParams
import org.libtorrent4j.TorrentInfo
import org.libtorrent4j.swig.libtorrent
import java.io.File
import java.util.concurrent.atomic.AtomicReference

class NoSeedingAfterCompletionTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test fun finishedDownloadIsPausedAndNeverUploads() {
        val root = File(instrumentation.targetContext.cacheDir, "no-seed-test-" + System.nanoTime()).apply { mkdirs() }
        val app = object : Application() {
            override fun getApplicationContext() = this
            override fun getCacheDir() = File(root, "cache").apply { mkdirs() }
            override fun getNoBackupFilesDir() = File(root, "private").apply { mkdirs() }
        }
        val engine = TorrentEngine(app)
        val error = AtomicReference<String?>(null)
        try {
            val link = seedUnmarkedCompleteFile(app, "complete.mkv")
            engine.startStreaming(link, onMetadataReceived = { _, _ -> }, onProgress = { _, _ -> },
                onError = { error.set(it) }, prioritizePlayback = false)

            await { engine.downloadProgress >= 100f || error.get() != null }

            assertEquals(null, error.get())
            assertNotNull("Finished torrent should stay available for verified reads", engine.selectedVideoFile)
            assertFalse("Finished torrent must not stay active as a seed", engine.isSharingTorrent)
            assertEquals(0L, engine.uploadedBytes)
        } finally {
            engine.stop()
            engine.awaitStoppedWrites()
            root.deleteRecursively()
        }
    }

    /** Writes a complete file without a completion record so the engine verifies it through libtorrent. */
    private fun seedUnmarkedCompleteFile(app: Application, name: String): String {
        val data = ByteArray(32768) { (it % 251).toByte() }
        val sha = java.security.MessageDigest.getInstance("SHA-1")
        val hashes = sha.digest(data.copyOfRange(0, 16384)) + sha.digest(data.copyOfRange(16384, 32768))
        val bytes = ("d4:infod6:lengthi32768e4:name" + name.length + ":" + name +
            "12:piece lengthi16384e6:pieces40:").toByteArray() + hashes + "7:privatei1eee".toByteArray()
        val link = libtorrent.make_magnet_uri(AddTorrentParams().apply { setTorrentInfo(TorrentInfo.bdecode(bytes)) }.swig())
        val key = MagnetIdentity.key(link)
        val storage = TorrentStorage.of(app)
        File(File(storage.dir(TorrentStorageDir.DOWNLOADS), key), name).apply { check(parentFile!!.mkdirs()); writeBytes(data) }
        TorrentCache(storage.dir(TorrentStorageDir.STATE), key, TorrentCacheCipher::encrypt, TorrentCacheCipher::decrypt)
            .write("metadata", bytes)
        return link
    }

    private fun await(check: () -> Boolean) {
        val limit = System.nanoTime() + 30_000_000_000L
        while (!check()) {
            check(System.nanoTime() < limit) { "Download did not finish" }
            Thread.sleep(50)
        }
    }
}
