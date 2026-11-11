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
class CompletedPlaybackSwitchTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private fun await(check: () -> Boolean) {
        val limit = System.nanoTime() + 20_000_000_000L
        while (!check()) { check(System.nanoTime() < limit) { "Playback switch stayed in preparation" }; Thread.sleep(50) }
    }
    @Test fun finishedBackgroundMp4CanBePlayedAfterAnotherVideoWithoutWaitingForStoppedWorker() {
        val root = File(instrumentation.targetContext.cacheDir, "switch-test-" + System.nanoTime()).apply { mkdirs() }
        val app = object : Application() {
            override fun getApplicationContext() = this
            override fun getCacheDir() = File(root, "cache").apply { mkdirs() }
            override fun getNoBackupFilesDir() = File(root, "private").apply { mkdirs() }
        }
        val model = MainViewModel(app)
        val data = ByteArray(32768)
        val buffer = java.nio.ByteBuffer.wrap(data)
        buffer.putInt(16).put("ftyp".toByteArray()).putLong(0).putInt(32744).put("mdat".toByteArray())
        buffer.position(32760); buffer.putInt(8).put("moov".toByteArray())
        val sha = java.security.MessageDigest.getInstance("SHA-1")
        val hashes = sha.digest(data.copyOfRange(0, 16384)) + sha.digest(data.copyOfRange(16384, 32768))
        fun seed(name: String, markComplete: Boolean): String {
            val bytes = ("d4:infod6:lengthi32768e4:name" + name.length + ":" + name +
                "12:piece lengthi16384e6:pieces40:").toByteArray() + hashes + "7:privatei1eee".toByteArray()
            val info = TorrentInfo.bdecode(bytes)
            val params = AddTorrentParams().apply { setTorrentInfo(info) }
            val link = libtorrent.make_magnet_uri(params.swig())
            val key = MagnetIdentity.key(link)
            val file = File(File(File(app.cacheDir, "torrent_downloads"), key), name)
            check(file.parentFile!!.mkdirs()); file.writeBytes(data)
            val cache = TorrentCache(File(app.cacheDir, "torrent_state"), key, TorrentCacheCipher::encrypt, TorrentCacheCipher::decrypt)
            cache.write("metadata", bytes)
            if (markComplete) cache.recordComplete(file, data.size.toLong())
            return link
        }
        try {
            val finished = seed("finished.mp4", false)
            val other = seed("other.mp4", true)
            instrumentation.runOnMainSync { model.startStreaming(finished, downloadOnly = true) }
            await { model.downloads.state.value.complete }
            await { !model.downloads.engine.isDownloading }
            instrumentation.runOnMainSync { model.startStreaming(other) }
            await { model.uiState.value == StreamUiState.Playing("other.mp4") }
            instrumentation.runOnMainSync { model.startStreaming(finished) }
            await { model.uiState.value == StreamUiState.Playing("finished.mp4") }
            assertSame(model.downloads.engineFor(MagnetIdentity.key(finished)), model.torrentEngine)
            await { model.torrentEngine.localVideo != null }
            instrumentation.runOnMainSync { model.startStreaming(other) }
            await { model.uiState.value == StreamUiState.Playing("other.mp4") }
            instrumentation.runOnMainSync { model.startStreaming(finished) }
            await { model.uiState.value == StreamUiState.Playing("finished.mp4") }
        } finally {
            instrumentation.runOnMainSync { model.stopStream(); model.downloads.stop() }
            model.downloads.awaitStoppedWrites()
            root.deleteRecursively()
        }
    }
}
