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
class PlaylistDownloadContinuationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private fun await(check: () -> Boolean) {
        val limit = System.nanoTime() + 25_000_000_000L
        while (!check()) { check(System.nanoTime() < limit) { "Download continuation timed out" }; Thread.sleep(50) }
    }
    @Test fun playlistRetainsSameDownloadMainStopsOnlyCurrentAndEarlyNavigationKeepsPendingWork() {
        val root = File(instrumentation.targetContext.cacheDir, "continuation-" + System.nanoTime()).apply { mkdirs() }
        val app = object : Application() {
            override fun getApplicationContext() = this
            override fun getCacheDir() = File(root, "cache").apply { mkdirs() }
            override fun getNoBackupFilesDir() = File(root, "private").apply { mkdirs() }
        }
        val model = MainViewModel(app)
        fun seed(name: String, complete: Boolean = false): String {
            val bytes = ("d4:infod6:lengthi32768e4:name" + name.length + ":" + name +
                "12:piece lengthi16384e6:pieces40:" + "\u0000".repeat(40) + "7:privatei1eee").toByteArray()
            val link = libtorrent.make_magnet_uri(AddTorrentParams().apply { setTorrentInfo(TorrentInfo.bdecode(bytes)) }.swig())
            val key = MagnetIdentity.key(link)
            val cache = TorrentCache(TorrentStorage.of(app).dir(TorrentStorageDir.STATE), key, TorrentCacheCipher::encrypt, TorrentCacheCipher::decrypt)
            cache.write("metadata", bytes)
            if (complete) {
                val file = File(File(TorrentStorage.of(app).dir(TorrentStorageDir.DOWNLOADS), key), name)
                check(file.parentFile!!.mkdirs()); file.writeBytes(ByteArray(32768)); cache.recordComplete(file, 32768)
            }
            return link
        }
        try {
            val other = seed("other.mkv"); val current = seed("current.mkv")
            val otherKey = MagnetIdentity.key(other); val currentKey = MagnetIdentity.key(current)
            instrumentation.runOnMainSync { model.startStreaming(other, downloadOnly = true); model.startStreaming(current) }
            await { model.uiState.value is StreamUiState.Buffering && model.torrentEngine.activeDownload.value != null }
            val source = model.torrentEngine; val session = source.nativeSessionIdentity
            instrumentation.runOnMainSync { model.continueInPlaylist() }
            assertEquals(StreamUiState.Idle, model.uiState.value)
            assertSame(source, model.downloads.engineFor(currentKey)); assertTrue(source.isDownloading)
            assertSame(session, source.nativeSessionIdentity); assertNotNull(source.activeDownload.value)
            instrumentation.runOnMainSync { model.startStreaming(current) }
            await { model.uiState.value is StreamUiState.Buffering }
            instrumentation.runOnMainSync { model.stopForMain() }
            assertNull(model.downloads.entry(currentKey)); assertFalse(source.isDownloading)
            assertTrue(model.downloads.engineFor(otherKey)!!.isDownloading)
            val early = seed("early.mkv"); val earlyKey = MagnetIdentity.key(early)
            instrumentation.runOnMainSync { model.startStreaming(early); model.continueInPlaylist() }
            await { model.downloads.engineFor(earlyKey)?.isDownloading == true }
            assertEquals(StreamUiState.Idle, model.uiState.value)
            val complete = seed("cached.mkv", true); val completeKey = MagnetIdentity.key(complete)
            instrumentation.runOnMainSync { model.startStreaming(complete); model.continueInPlaylist() }
            await { model.downloads.entry(completeKey)?.phase == DownloadPhase.COMPLETE }
            assertEquals(StreamUiState.Idle, model.uiState.value)
        } finally {
            instrumentation.runOnMainSync { model.stopStream(); model.stopDownloads() }
            model.downloads.awaitStoppedWrites(); root.deleteRecursively()
        }
    }
}
