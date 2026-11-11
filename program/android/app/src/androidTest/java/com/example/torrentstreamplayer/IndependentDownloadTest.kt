package com.example.torrentstreamplayer

import com.example.torrentstreamplayer.engine.TorrentStorage
import com.example.torrentstreamplayer.meta.TorrentStorageDir
import android.app.Application
import androidx.test.platform.app.InstrumentationRegistry
import com.example.torrentstreamplayer.engine.*
import com.example.torrentstreamplayer.ui.*
import org.junit.Assert.*
import org.junit.Test
import org.libtorrent4j.*
import org.libtorrent4j.swig.libtorrent
import java.io.File
import kotlinx.coroutines.runBlocking
class IndependentDownloadTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private fun waitUntil(check: () -> Boolean) {
        val deadline = System.nanoTime() + 20_000_000_000L
        while (!check()) {
            check(System.nanoTime() < deadline) { "Timed out waiting for independent download/playback" }
            Thread.sleep(50)
        }
    }
    @Test fun playbackAndReturningToInputKeepDownloadEngineAlive() {
        val root = File(instrumentation.targetContext.cacheDir, "independent-test-" + System.nanoTime()).apply { mkdirs() }
        val app = object : Application() {
            override fun getApplicationContext() = this
            override fun getCacheDir() = File(root, "cache").apply { mkdirs() }
            override fun getNoBackupFilesDir() = File(root, "private").apply { mkdirs() }
        }
        val model = MainViewModel(app)
        fun seed(name: String, complete: Boolean): String {
            val metadata = ("d4:infod6:lengthi32768e4:name" + name.length + ":" + name +
                "12:piece lengthi16384e6:pieces40:" + "\u0000".repeat(40) + "7:privatei1eee").toByteArray()
            val params = AddTorrentParams().apply { setTorrentInfo(TorrentInfo.bdecode(metadata)) }
            val link = libtorrent.make_magnet_uri(params.swig())
            val key = MagnetIdentity.key(link)
            val cache = TorrentCache(TorrentStorage.of(app).dir(TorrentStorageDir.STATE), key, TorrentCacheCipher::encrypt, TorrentCacheCipher::decrypt)
            cache.write("metadata", metadata)
            if (complete) {
                val file = File(File(TorrentStorage.of(app).dir(TorrentStorageDir.DOWNLOADS), key), name)
                check(file.parentFile!!.mkdirs()); file.writeBytes(ByteArray(32768))
                cache.recordComplete(file, 32768)
            }
            return link
        }
        try {
            val downloading = seed("first.mkv", false)
            val completed = seed("second.mkv", true)
            instrumentation.runOnMainSync { model.startStreaming(downloading, downloadOnly = true) }
            waitUntil { model.downloads.engine.isDownloading && model.downloads.engine.activeDownload.value != null }
            val engine = model.downloads.engine
            instrumentation.runOnMainSync { model.startStreaming(completed) }
            waitUntil { model.uiState.value is StreamUiState.Playing }
            assertNotSame(engine, model.torrentEngine)
            assertTrue(engine.isDownloading)
            instrumentation.runOnMainSync { model.stopStream() }
            assertTrue(engine.isDownloading)
            assertEquals(MagnetIdentity.key(downloading), model.downloads.state.value.key)
            instrumentation.runOnMainSync { model.startStreaming(downloading) }
            waitUntil { model.uiState.value is StreamUiState.Buffering }
            assertSame(engine, model.torrentEngine)
            assertTrue(engine.isDownloading)
            instrumentation.runOnMainSync { model.stopStream() }
            assertTrue(engine.isDownloading)
            assertTrue(engine.activeDownload.value != null)
            runBlocking { model.deleteSavedMagnet(completed) }
            assertTrue(engine.isDownloading)
            assertFalse(File(TorrentStorage.of(app).dir(TorrentStorageDir.DOWNLOADS), MagnetIdentity.key(completed)).exists())
            assertFalse(File(TorrentStorage.of(app).dir(TorrentStorageDir.STATE), MagnetIdentity.key(completed)).exists())
            assertTrue(model.savedMagnets().none { it.magnet == completed })
            runBlocking { model.deleteSavedMagnet(downloading) }
            assertFalse(engine.isDownloading)
            assertNull(model.downloads.state.value.key)
            Thread.sleep(200)
            assertFalse(File(TorrentStorage.of(app).dir(TorrentStorageDir.DOWNLOADS), MagnetIdentity.key(downloading)).exists())
            assertFalse(File(TorrentStorage.of(app).dir(TorrentStorageDir.STATE), MagnetIdentity.key(downloading)).exists())
            assertTrue(model.savedMagnets().isEmpty())
        } finally {
            instrumentation.runOnMainSync { model.stopStream(); model.downloads.stop() }
            root.deleteRecursively()
        }
    }
}
