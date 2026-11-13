package com.example.torrentstreamplayer

import android.app.Application
import androidx.test.platform.app.InstrumentationRegistry
import com.example.torrentstreamplayer.engine.DownloadOrderPolicy
import com.example.torrentstreamplayer.engine.MagnetIdentity
import com.example.torrentstreamplayer.engine.TorrentCache
import com.example.torrentstreamplayer.engine.TorrentCacheCipher
import com.example.torrentstreamplayer.engine.TorrentEngine
import com.example.torrentstreamplayer.engine.TorrentStorage
import com.example.torrentstreamplayer.meta.TorrentStorageDir
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.libtorrent4j.AddTorrentParams
import org.libtorrent4j.TorrentInfo
import org.libtorrent4j.swig.libtorrent
import java.io.File
import java.util.concurrent.atomic.AtomicReference

/**
 * Download order on the real libtorrent session (#150): a download-only task keeps native sequential order with its
 * edge pieces on top, playback switches to the deadline window, and handing playback back restores both. Playback
 * priorities are only applied once the task's own file and edge priorities are set, so they are not overwritten.
 */
class DownloadOrderNativeTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val pieceCount = 8
    private val top = 7
    private val normal = 4

    @Test fun downloadOnlyIsSequentialPlaybackUsesDeadlinesAndReleaseRestoresOrder() {
        val root = File(instrumentation.targetContext.cacheDir, "download-order-" + System.nanoTime()).apply { mkdirs() }
        val app = object : Application() {
            override fun getApplicationContext() = this
            override fun getCacheDir() = File(root, "cache").apply { mkdirs() }
            override fun getNoBackupFilesDir() = File(root, "private").apply { mkdirs() }
        }
        val engine = TorrentEngine(app)
        val error = AtomicReference<String?>(null)
        try {
            assertFalse("no run yet: playback must not take priority", engine.requestPlayback())
            engine.startStreaming(cachedTorrent(app, "order.mkv"), onMetadataReceived = { _, _ -> },
                onProgress = { _, _ -> }, onError = { error.set(it) }, prioritizePlayback = false)

            // Until the task has set its own priorities, playback is refused; once accepted they are in place.
            await { engine.requestPlayback() || error.get() != null }
            assertNull(error.get())
            assertFalse("playback turns native sequential off", engine.sequentialDownload)
            (0 until pieceCount).forEach { assertEquals("piece $it in the startup buffer", top, engine.piecePriorityOf(it)) }

            engine.releasePlaybackPriority()
            assertTrue("released task downloads from the file start again", engine.sequentialDownload)
            val edges = DownloadOrderPolicy.edgePieces(0, pieceCount - 1)
            (0 until pieceCount).forEach {
                assertEquals("piece $it after release", if (it in edges) top else normal, engine.piecePriorityOf(it))
            }
        } finally {
            engine.stop()
            engine.awaitStoppedWrites()
            root.deleteRecursively()
        }
    }

    @Test fun downloadOnlyTaskStartsSequentialWithEdgePiecesOnTop() {
        val root = File(instrumentation.targetContext.cacheDir, "download-order-" + System.nanoTime()).apply { mkdirs() }
        val app = object : Application() {
            override fun getApplicationContext() = this
            override fun getCacheDir() = File(root, "cache").apply { mkdirs() }
            override fun getNoBackupFilesDir() = File(root, "private").apply { mkdirs() }
        }
        val engine = TorrentEngine(app)
        try {
            engine.startStreaming(cachedTorrent(app, "order.mkv"), onMetadataReceived = { _, _ -> },
                onProgress = { _, _ -> }, onError = { }, prioritizePlayback = false)
            await { engine.isDownloading && engine.sequentialDownload }
            val edges = DownloadOrderPolicy.edgePieces(0, pieceCount - 1)
            await { edges.all { engine.piecePriorityOf(it) == top } }
            assertEquals("middle pieces keep the file priority", normal, engine.piecePriorityOf(pieceCount / 2))
        } finally {
            engine.stop()
            engine.awaitStoppedWrites()
            root.deleteRecursively()
        }
    }

    /** A private torrent with no peers whose metadata is cached, so the task never finishes during the test. */
    private fun cachedTorrent(app: Application, name: String): String {
        val pieceLength = 16384
        val bytes = ("d4:infod6:lengthi${pieceLength * pieceCount}e4:name${name.length}:$name" +
            "12:piece lengthi${pieceLength}e6:pieces${20 * pieceCount}:").toByteArray() +
            ByteArray(20 * pieceCount) + "7:privatei1eee".toByteArray()
        val link = libtorrent.make_magnet_uri(AddTorrentParams().apply { setTorrentInfo(TorrentInfo.bdecode(bytes)) }.swig())
        TorrentCache(TorrentStorage.of(app).dir(TorrentStorageDir.STATE), MagnetIdentity.key(link),
            TorrentCacheCipher::encrypt, TorrentCacheCipher::decrypt).write("metadata", bytes)
        return link
    }

    private fun await(check: () -> Boolean) {
        val limit = System.nanoTime() + 20_000_000_000L
        while (!check()) {
            check(System.nanoTime() < limit) { "Timed out waiting for the download order" }
            Thread.sleep(50)
        }
    }
}
