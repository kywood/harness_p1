package com.example.torrentstreamplayer

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.os.StrictMode
import android.os.strictmode.Violation
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.example.torrentstreamplayer.engine.MagnetIdentity
import com.example.torrentstreamplayer.engine.TorrentCache
import com.example.torrentstreamplayer.engine.TorrentCacheCipher
import com.example.torrentstreamplayer.engine.TorrentEngine
import com.example.torrentstreamplayer.engine.TorrentStorage
import com.example.torrentstreamplayer.meta.TorrentStorageDir
import com.example.torrentstreamplayer.ui.MainViewModel
import com.example.torrentstreamplayer.ui.StreamUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.libtorrent4j.AddTorrentParams
import org.libtorrent4j.TorrentInfo
import org.libtorrent4j.swig.libtorrent
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Button-driven flows must never call blocking engine methods on the main thread.
 *
 * Blocking engine entry points call StrictMode.noteSlowCall, so this check does not depend on how
 * busy libtorrent is (on a quiet emulator those calls return fast; with many peers they do not).
 * A main-looper probe additionally records the longest stall.
 */
class MainThreadResponsivenessTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val main = Handler(Looper.getMainLooper())

    @Test fun detectorCatchesBlockingEngineCallOnMainThread() {
        withEngine { app ->
            val engine = TorrentEngine(app)
            try {
                val violations = watchMainThread { instrumentation.runOnMainSync { engine.isPlaybackReady() } }
                assertTrue("expected a slow-call violation", violations.any { "TorrentEngine.isPlaybackReady" in it })
            } finally {
                engine.stop()
                engine.awaitStoppedWrites()
            }
        }
    }

    @Test fun buttonFlowsKeepEngineWorkOffMainThread() = withEngine { app ->
        lateinit var model: MainViewModel
        instrumentation.runOnMainSync { model = MainViewModel(app, FixedConnection()) }
        val stalls = linkedMapOf<String, Long>()
        try {
            val links = (1..3).map { largeTorrent(app, "movie-$it.mp4") }
            val violations = watchMainThread {
                stalls["play -> buffering"] = probe {
                    instrumentation.runOnMainSync { model.startStreaming(links[0]) }
                    await { model.uiState.value is StreamUiState.Buffering }
                    Thread.sleep(1_000) // Several 250 ms readiness polls while buffering.
                }
                stalls["back while buffering"] = probe {
                    instrumentation.runOnMainSync { model.stopForMain() }
                    Thread.sleep(1_000)
                }
                stalls["start 3 downloads"] = probe {
                    instrumentation.runOnMainSync { links.forEach { model.startStreaming(it, downloadOnly = true) } }
                    Thread.sleep(2_000)
                }
                stalls["stop all downloads"] = probe {
                    instrumentation.runOnMainSync { model.stopDownloads() }
                    Thread.sleep(1_000)
                }
            }
            Log.i(TAG, "worst main-thread stall: " + stalls.entries.joinToString { "${it.key}=${it.value}ms" })
            assertEquals("blocking engine calls on the main thread", emptyList<String>(), violations)
            assertTrue("main thread stalled: $stalls", stalls.values.all { it < STALL_BUDGET_MS })
        } finally {
            instrumentation.runOnMainSync { model.stopStream(); model.downloads.stop() }
            model.downloads.awaitStoppedWrites(model.downloads.stoppedWriters())
        }
    }

    /** Runs [block] with a main-thread StrictMode policy that records custom slow calls. */
    private fun watchMainThread(block: () -> Unit): List<String> {
        val violations = CopyOnWriteArrayList<String>()
        lateinit var previous: StrictMode.ThreadPolicy
        instrumentation.runOnMainSync {
            previous = StrictMode.getThreadPolicy()
            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().detectCustomSlowCalls()
                .penaltyListener(Runnable::run) { v: Violation -> violations += v.message.orEmpty() }.build())
        }
        try { block() } finally { instrumentation.runOnMainSync { StrictMode.setThreadPolicy(previous) } }
        return violations.toList()
    }

    /** Returns the longest delay of a main-looper message posted every 10 ms while [flow] runs. */
    private fun probe(flow: () -> Unit): Long {
        val worst = AtomicLong()
        val running = AtomicBoolean(true)
        val prober = Thread {
            while (running.get()) {
                val posted = System.nanoTime()
                val done = CountDownLatch(1)
                main.post { worst.accumulateAndGet((System.nanoTime() - posted) / 1_000_000, ::maxOf); done.countDown() }
                done.await()
                Thread.sleep(10)
            }
        }.apply { start() }
        try { flow() } finally { running.set(false); prober.join(15_000) }
        return worst.get()
    }

    private fun <T> withEngine(test: (Application) -> T): T {
        val root = File(instrumentation.targetContext.cacheDir, "main-thread-" + System.nanoTime()).apply { mkdirs() }
        val app = object : Application() {
            override fun getApplicationContext() = this
            override fun getCacheDir() = File(root, "cache").apply { mkdirs() }
            override fun getNoBackupFilesDir() = File(root, "private").apply { mkdirs() }
        }
        try { return test(app) } finally { root.deleteRecursively() }
    }

    private fun await(check: () -> Boolean) {
        val limit = System.nanoTime() + 30_000_000_000L
        while (!check()) {
            check(System.nanoTime() < limit) { "state not reached" }
            Thread.sleep(50)
        }
    }

    /** A 4,000-piece torrent whose data is absent, so playback stays buffering with no peers. */
    private fun largeTorrent(app: Application, name: String): String {
        val pieces = 4_000
        val hashes = ByteArray(pieces * 20) { (it * 31 + name.hashCode()).toByte() }
        val bytes = ("d4:infod6:lengthi" + pieces * PIECE + "e4:name" + name.length + ":" + name +
            "12:piece lengthi" + PIECE + "e6:pieces" + hashes.size + ":").toByteArray() + hashes + "7:privatei1eee".toByteArray()
        val link = libtorrent.make_magnet_uri(AddTorrentParams().apply { setTorrentInfo(TorrentInfo.bdecode(bytes)) }.swig())
        TorrentCache(TorrentStorage.of(app).dir(TorrentStorageDir.STATE), MagnetIdentity.key(link),
            TorrentCacheCipher::encrypt, TorrentCacheCipher::decrypt).write("metadata", bytes)
        return link
    }

    private companion object {
        const val TAG = "MainThreadResponsiveness"
        const val PIECE = 16_384
        /**
         * Coarse guard only: emulator scheduling alone can delay the probe by a few hundred ms on a busy
         * host. The StrictMode slow-call check above is the deterministic part of this test.
         */
        const val STALL_BUDGET_MS = 500L
    }
}
