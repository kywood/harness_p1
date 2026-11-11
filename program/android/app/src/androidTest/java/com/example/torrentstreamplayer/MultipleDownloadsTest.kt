package com.example.torrentstreamplayer
import android.app.Application
import androidx.test.platform.app.InstrumentationRegistry
import com.example.torrentstreamplayer.engine.*
import com.example.torrentstreamplayer.ui.*
import org.junit.Assert.*
import org.junit.Test
import org.libtorrent4j.*
import org.libtorrent4j.swig.libtorrent
import java.io.File
class MultipleDownloadsTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private fun await(check: () -> Boolean) {
        val limit = System.nanoTime() + 30_000_000_000L
        while (!check()) { check(System.nanoTime() < limit) { "Multiple download transition timed out" }; Thread.sleep(50) }
    }
    @Test fun concurrentQueuePlaybackStopDeleteAndCompletionRemainIndependent() {
        val root = File(instrumentation.targetContext.cacheDir, "multi-test-" + System.nanoTime()).apply { mkdirs() }
        val app = object : Application() {
            override fun getApplicationContext() = this
            override fun getCacheDir() = File(root, "cache").apply { mkdirs() }
            override fun getNoBackupFilesDir() = File(root, "private").apply { mkdirs() }
        }
        val model = MainViewModel(app)
        fun seed(name: String, complete: Boolean = false): String {
            val bytes = ("d4:infod6:lengthi32768e4:name" + name.length + ":" + name +
                "12:piece lengthi16384e6:pieces40:" + "\u0000".repeat(40) + "7:privatei1eee").toByteArray()
            val params = AddTorrentParams().apply { setTorrentInfo(TorrentInfo.bdecode(bytes)) }
            val link = libtorrent.make_magnet_uri(params.swig()); val key = MagnetIdentity.key(link)
            val cache = TorrentCache(File(app.cacheDir, "torrent_state"), key, TorrentCacheCipher::encrypt, TorrentCacheCipher::decrypt)
            cache.write("metadata", bytes)
            if (complete) {
                val file = File(File(File(app.cacheDir, "torrent_downloads"), key), name)
                check(file.parentFile!!.mkdirs()); file.writeBytes(ByteArray(32768)); cache.recordComplete(file, 32768)
            }
            return link
        }
        try {
            val links = (1..4).map { seed("video-$it.mkv") }
            val keys = links.map(MagnetIdentity::key)
            instrumentation.runOnMainSync { links.forEach { model.startStreaming(it, downloadOnly = true) } }
            await { model.downloads.tasks.value.values.count { it.phase == DownloadPhase.RUNNING } == 3 &&
                model.downloads.entry(keys[3])?.phase == DownloadPhase.QUEUED }
            await { keys.take(3).all { model.downloads.engineFor(it)?.isDownloading == true } }
            val sharedSession = model.downloads.engineFor(keys[0])!!.nativeSessionIdentity
            assertNotNull(sharedSession)
            assertSame(sharedSession, model.downloads.engineFor(keys[1])!!.nativeSessionIdentity)
            assertSame(sharedSession, model.downloads.engineFor(keys[2])!!.nativeSessionIdentity)
            instrumentation.runOnMainSync { model.startStreaming(links.first() + "&tr=https%3A%2F%2Finvalid.test", downloadOnly = true) }
            assertEquals(4, model.downloads.tasks.value.size)
            instrumentation.runOnMainSync { model.startStreaming(links[3]) }
            await { model.uiState.value is StreamUiState.Buffering }
            assertSame(model.downloads.engineFor(keys[3]), model.torrentEngine)
            assertEquals(DownloadPhase.QUEUED, model.downloads.entry(keys[2])?.phase)
            assertEquals(3, model.downloads.tasks.value.values.count { it.phase == DownloadPhase.RUNNING })
            instrumentation.runOnMainSync { model.stopStream(); model.stopDownload(keys[1]) }
            await { model.downloads.entry(keys[2])?.phase == DownloadPhase.RUNNING &&
                model.downloads.engineFor(keys[2])?.isDownloading == true }
            assertTrue(model.downloads.engineFor(keys[0])!!.isDownloading)
            assertTrue(model.downloads.engineFor(keys[3])!!.isDownloading)
            assertSame(sharedSession, model.downloads.engineFor(keys[3])!!.nativeSessionIdentity)
            model.deleteSavedMagnet(links[0])
            assertNull(model.downloads.entry(keys[0]))
            assertTrue(model.downloads.engineFor(keys[2])!!.isDownloading)
            assertTrue(model.downloads.engineFor(keys[3])!!.isDownloading)
            assertFalse(File(app.cacheDir, "torrent_downloads/" + keys[0]).exists())
            instrumentation.runOnMainSync { model.stopDownloads() }
            assertTrue(model.downloads.tasks.value.isEmpty())
            val done = seed("complete.mkv", true)
            val waiting = (1..3).map { seed("next-$it.mkv") }
            instrumentation.runOnMainSync {
                model.startStreaming(done, downloadOnly = true)
                waiting.forEach { model.startStreaming(it, downloadOnly = true) }
                assertEquals(DownloadPhase.QUEUED, model.downloads.entry(MagnetIdentity.key(waiting.last()))?.phase)
            }
            await { model.downloads.entry(MagnetIdentity.key(done))?.phase == DownloadPhase.COMPLETE &&
                waiting.all { model.downloads.engineFor(MagnetIdentity.key(it))?.isDownloading == true } }
            assertEquals(3, model.downloads.tasks.value.values.count { it.phase == DownloadPhase.RUNNING })
            assertEquals(0, model.downloads.tasks.value.values.count { it.phase == DownloadPhase.QUEUED })
            val stoppedKeys = waiting.take(2).map(MagnetIdentity::key).toSet()
            instrumentation.runOnMainSync { model.stopDownloads(stoppedKeys) }
            assertTrue(stoppedKeys.none { model.downloads.entry(it) != null })
            assertTrue(model.downloads.engineFor(MagnetIdentity.key(waiting.last()))!!.isDownloading)
        } finally {
            instrumentation.runOnMainSync { model.stopStream(); model.stopDownloads() }
            model.downloads.awaitStoppedWrites()
            root.deleteRecursively()
        }
    }
}
