package com.example.torrentstreamplayer

import android.app.Application
import androidx.test.platform.app.InstrumentationRegistry
import com.example.torrentstreamplayer.engine.*
import com.example.torrentstreamplayer.ui.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.libtorrent4j.*
import org.libtorrent4j.swig.libtorrent
import java.io.File

class QuickDownloadResumeTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private fun await(check: () -> Boolean) {
        val deadline = System.nanoTime() + 20000000000L
        while (!check()) { check(System.nanoTime() < deadline); Thread.sleep(50) }
    }
    @Test fun linkDownloadKeepsPlaybackAndResumePersistsAndResetsIndependently() {
        val root = File(instrumentation.targetContext.cacheDir, "quick-resume-" + System.nanoTime()).apply { mkdirs() }
        val app = object : Application() {
            override fun getApplicationContext() = this
            override fun getCacheDir() = File(root, "cache").apply { mkdirs() }
            override fun getNoBackupFilesDir() = File(root, "private").apply { mkdirs() }
        }
        fun metadata(name: String) = ("d4:infod6:lengthi32768e4:name" + name.length + ":" + name +
            "12:piece lengthi16384e6:pieces40:" + "\u0000".repeat(40) + "7:privatei1eee").toByteArray()
        fun link(bytes: ByteArray) = libtorrent.make_magnet_uri(AddTorrentParams().apply { setTorrentInfo(TorrentInfo.bdecode(bytes)) }.swig())
        val playingBytes = metadata("playing.mkv")
        val playing = link(playingBytes); val key = MagnetIdentity.key(playing)
        val cache = TorrentCache(File(app.cacheDir, "torrent_state"), key, TorrentCacheCipher::encrypt, TorrentCacheCipher::decrypt)
        cache.write("metadata", playingBytes)
        val video = File(File(File(app.cacheDir, "torrent_downloads"), key), "playing.mkv")
        video.parentFile!!.mkdirs(); video.writeBytes(ByteArray(32768)); cache.recordComplete(video, 32768)
        val extra = metadata("extra.mkv"); val extraKey = MagnetIdentity.key(link(extra))
        val server = object : fi.iki.elonen.NanoHTTPD(0) {
            override fun serve(session: IHTTPSession) = newFixedLengthResponse(Response.Status.OK,
                "application/x-bittorrent", extra.inputStream(), extra.size.toLong())
        }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        val model = MainViewModel(app)
        server.start()
        try {
            instrumentation.runOnMainSync { model.startStreaming(playing) }
            await { model.uiState.value is StreamUiState.Playing }
            val source = model.torrentEngine
            instrumentation.runOnMainSync { model.playbackPositionWriter()(42000, 120000) }
            await { model.savedMagnets().first { MagnetIdentity.key(it.magnet) == key }.lastPositionMs == 42000L }
            val queued = CompletableDeferred<Unit>()
            instrumentation.runOnMainSync { scope.launch {
                try { model.queueDownload("http://127.0.0.1:" + server.listeningPort + "/extra.torrent"); queued.complete(Unit) }
                catch (failure: Throwable) { queued.completeExceptionally(failure) }
            } }
            runBlocking { withTimeout(10000) { queued.await() } }
            await { model.downloads.engineFor(extraKey)?.isDownloading == true }
            assertTrue(model.uiState.value is StreamUiState.Playing)
            assertSame(source, model.torrentEngine)
            instrumentation.runOnMainSync { model.stopStream(); model.startStreaming(playing) }
            await { model.uiState.value is StreamUiState.Playing }
            assertEquals(42000, model.resumePositionMs)
            val reset = CompletableDeferred<Unit>()
            instrumentation.runOnMainSync { scope.launch { model.resetPlaybackPosition(playing); reset.complete(Unit) } }
            runBlocking { withTimeout(10000) { reset.await() } }
            assertEquals(0, model.savedMagnets().first { MagnetIdentity.key(it.magnet) == key }.lastPositionMs)
            assertTrue(model.downloads.engineFor(extraKey)!!.isDownloading)
        } finally {
            server.stop(); scope.cancel()
            instrumentation.runOnMainSync { model.stopStream(); model.stopDownloads() }
            model.downloads.awaitStoppedWrites(); root.deleteRecursively()
        }
    }
}
