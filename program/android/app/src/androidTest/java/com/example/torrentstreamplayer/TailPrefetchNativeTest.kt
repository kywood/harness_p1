package com.example.torrentstreamplayer

import android.app.Application
import androidx.test.platform.app.InstrumentationRegistry
import com.example.torrentstreamplayer.engine.MagnetIdentity
import com.example.torrentstreamplayer.engine.StartupPrefetchPlan
import com.example.torrentstreamplayer.engine.TorrentCache
import com.example.torrentstreamplayer.engine.TorrentCacheCipher
import com.example.torrentstreamplayer.engine.TorrentEngine
import com.example.torrentstreamplayer.engine.TorrentStorage
import com.example.torrentstreamplayer.meta.TorrentStorageDir
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.libtorrent4j.AddTorrentParams
import org.libtorrent4j.TorrentInfo
import org.libtorrent4j.swig.libtorrent
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicReference

/**
 * File-end prefetch on the real libtorrent session (#150). Only the pieces written to disk are verified by
 * libtorrent's file check, so each case controls what the startup index probe can read.
 *
 * libtorrent has no getter for piece deadlines, so the late tail deadline itself is not asserted: these tests check
 * the pinned set, the prefetch set and piece priorities (a pinned piece is 7, an unpinned one falls back to 1 while
 * the playback buffer is missing). The deadline value is covered by `StartupPrefetchPlanTest`.
 */
class TailPrefetchNativeTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val pieceLength = 16 * 1024
    private val pieceCount = 512 // 8 MB: a 2 MB head and a 4 MB MP4 tail that do not overlap.
    private val size = pieceLength * pieceCount
    private val top = 7
    private val background = 1
    private val tail = StartupPrefetchPlan.tailPieces(size.toLong(), 0, pieceLength.toLong(), "mp4")
    private val headPieces = 0 until 128

    @Test fun faststartMp4DropsTheTailOnceTheHeadShowsTheIndex() = withEngine("front.mp4", mp4(moovAtFront = true),
        written = 0..0) { engine ->
        await { engine.tailPrefetchPieces.isEmpty() }
        await { engine.pinnedPieces.none { it in tail } }
        await { engine.piecePriorityOf(tail.last()) == background }
        assertEquals(background, engine.piecePriorityOf(tail.first()))
    }

    @Test fun mp4WithTheIndexAtTheBackKeepsTheTailPinned() = withEngine("back.mp4", mp4(moovAtFront = false),
        written = 0..0) { engine ->
        // The probe reads ftyp and the mdat header from piece 0, then needs the moov header past the head: BACK.
        await { engine.pinnedPieces.containsAll(tail) }
        Thread.sleep(2_500) // Several readiness checks and download loops: the tail must stay pinned.
        assertEquals(tail, engine.tailPrefetchPieces)
        assertTrue(engine.pinnedPieces.containsAll(tail))
        tail.forEach { assertEquals("tail piece $it", top, engine.piecePriorityOf(it)) }
    }

    @Test fun matroskaTailStaysPinnedWhileTheIndexLocationIsUnknown() {
        val mkvTail = StartupPrefetchPlan.tailPieces(size.toLong(), 0, pieceLength.toLong(), "mkv")
        withEngine("unknown.mkv", content(), written = IntRange.EMPTY) { engine ->
            await { engine.pinnedPieces.containsAll(mkvTail) }
            assertEquals(mkvTail, engine.tailPrefetchPieces)
            mkvTail.forEach { assertEquals("tail piece $it", top, engine.piecePriorityOf(it)) }
        }
    }

    @Test fun playingClearsThePrefetchSoSeeksDoNotPinTheTailAgain() {
        val mkvTail = StartupPrefetchPlan.tailPieces(size.toLong(), 0, pieceLength.toLong(), "mkv")
        withEngine("ready.mkv", content(), written = headPieces) { engine ->
            await { engine.isPlaybackReady() }
            assertTrue(engine.tailPrefetchPieces.isEmpty())
            await { engine.pinnedPieces.none { it in mkvTail } }
            engine.beginSeek(targetMs = 1_000, durationMs = 10_000)
            Thread.sleep(1_500)
            assertTrue(engine.tailPrefetchPieces.isEmpty())
            assertTrue(engine.pinnedPieces.none { it in mkvTail })
        }
    }

    /** Deterministic non-zero bytes, so unwritten (zero) pieces never pass the hash check. */
    private fun content(): ByteArray = ByteArray(size) { ((it * 31 + 7) % 251 + 1).toByte() }

    /** An MP4 of top-level boxes: ftyp, then moov and mdat in the requested order. */
    private fun mp4(moovAtFront: Boolean): ByteArray {
        val data = content()
        val moovSize = 64 * 1024
        val ftypSize = 16
        fun box(at: Int, length: Int, type: String) {
            ByteBuffer.wrap(data, at, 8).putInt(length).put(type.toByteArray(Charsets.ISO_8859_1))
        }
        box(0, ftypSize, "ftyp")
        if (moovAtFront) {
            box(ftypSize, moovSize, "moov")
            box(ftypSize + moovSize, size - ftypSize - moovSize, "mdat")
        } else {
            box(ftypSize, size - ftypSize - moovSize, "mdat")
            box(size - moovSize, moovSize, "moov")
        }
        return data
    }

    private fun withEngine(name: String, data: ByteArray, written: IntRange, test: (TorrentEngine) -> Unit) {
        val root = File(instrumentation.targetContext.cacheDir, "tail-prefetch-" + System.nanoTime()).apply { mkdirs() }
        val app = object : Application() {
            override fun getApplicationContext() = this
            override fun getCacheDir() = File(root, "cache").apply { mkdirs() }
            override fun getNoBackupFilesDir() = File(root, "private").apply { mkdirs() }
        }
        val engine = TorrentEngine(app)
        val error = AtomicReference<String?>(null)
        try {
            val link = seed(app, name, data, written)
            engine.startStreaming(link, onMetadataReceived = { _, _ -> }, onProgress = { _, _ -> },
                onError = { error.set(it) }, prioritizePlayback = false)
            await { engine.requestPlayback() || error.get() != null }
            assertNull(error.get())
            test(engine)
        } finally {
            engine.stop()
            engine.awaitStoppedWrites()
            root.deleteRecursively()
        }
    }

    /** Caches the metadata of a private torrent with no peers and writes only the [written] pieces of [data]. */
    private fun seed(app: Application, name: String, data: ByteArray, written: IntRange): String {
        val sha = MessageDigest.getInstance("SHA-1")
        val hashes = (0 until pieceCount).fold(ByteArray(0)) { all, piece ->
            all + sha.digest(data.copyOfRange(piece * pieceLength, (piece + 1) * pieceLength))
        }
        val bytes = ("d4:infod6:lengthi${size}e4:name${name.length}:$name" +
            "12:piece lengthi${pieceLength}e6:pieces${hashes.size}:").toByteArray() + hashes + "7:privatei1eee".toByteArray()
        val link = libtorrent.make_magnet_uri(AddTorrentParams().apply { setTorrentInfo(TorrentInfo.bdecode(bytes)) }.swig())
        val key = MagnetIdentity.key(link)
        val storage = TorrentStorage.of(app)
        if (!written.isEmpty()) {
            val file = File(File(storage.dir(TorrentStorageDir.DOWNLOADS), key), name).apply { check(parentFile!!.mkdirs()) }
            RandomAccessFile(file, "rw").use { out ->
                out.setLength(size.toLong())
                for (piece in written) { out.seek(piece.toLong() * pieceLength); out.write(data, piece * pieceLength, pieceLength) }
            }
        }
        TorrentCache(storage.dir(TorrentStorageDir.STATE), key, TorrentCacheCipher::encrypt, TorrentCacheCipher::decrypt)
            .write("metadata", bytes)
        return link
    }

    private fun await(check: () -> Boolean) {
        val limit = System.nanoTime() + 30_000_000_000L
        while (!check()) {
            check(System.nanoTime() < limit) { "Timed out waiting for the tail prefetch state" }
            Thread.sleep(50)
        }
    }
}
