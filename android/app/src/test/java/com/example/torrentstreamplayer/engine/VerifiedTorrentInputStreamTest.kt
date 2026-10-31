package com.example.torrentstreamplayer.engine

import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.io.File
import java.util.concurrent.*

class VerifiedTorrentInputStreamTest {
    @Test fun verifiedReadsDoNotWaitForOrRenewDownloadRequests() = withFile { file ->
        stream(file, { true }, request = { fail("Verified data must be read immediately") }).use {
            assertArrayEquals(file.readBytes(), it.readBytes())
        }
    }
    @Test fun waitingReadRenewsPriorityIfAnotherRequestDisplacesIt() = withFile { file ->
        var ready = false
        var requests = 0
        stream(file, { ready }, request = { if (++requests == 2) ready = true }, timeout = 2000).use {
            assertEquals(1, it.read())
        }
        assertEquals(2, requests)
    }
    @Test fun downloadedSeekRangeAdvancesPrefetchWithoutDownloadingItAgain() = withFile { file ->
        val positions = mutableListOf<Long>()
        VerifiedTorrentInputStream(file, 4, 8, { true }, { fail("Verified bytes need no download") },
            { true }, { _, count -> minOf(count, 2) }, prefetch = { positions.add(it) }).use {
            assertArrayEquals(byteArrayOf(5, 6, 7, 8), it.readBytes())
        }
        assertEquals(listOf(4L, 6L), positions)
    }
    private fun stream(file: File, ready: (Long) -> Boolean, active: () -> Boolean = { true },
        request: (Long) -> Unit = {}, timeout: Long = 100): VerifiedTorrentInputStream =
        VerifiedTorrentInputStream(file, 0, 8, ready, request, active,
            { pos, count -> TorrentPlayback.readSize(pos, count, 8, 2, 4) }, timeout)
    private fun withFile(test: (File) -> Unit) {
        val file = File.createTempFile("torrent-read", ".bin")
        try { file.writeBytes(ByteArray(8) { (it + 1).toByte() }); test(file) }
        finally { file.delete() }
    }
    @Test fun readsOnlyVerifiedPieceAndStopsAtBoundary() = withFile { file ->
        stream(file, { it < 2 }).use { input ->
            val output = ByteArray(8)
            assertEquals(2, input.read(output))
            assertArrayEquals(byteArrayOf(1, 2), output.copyOf(2))
            assertThrows(IOException::class.java) { input.read(output) }
        }
    }
    @Test fun preallocatedFileDoesNotMakeMissingPieceReadable() = withFile { file ->
        java.io.RandomAccessFile(file, "rw").use { it.setLength(1024) }
        stream(file, { false }).use { input ->
            assertThrows(IOException::class.java) { input.read() }
        }
    }
    @Test fun completedStreamReturnsExactBytesAndEof() = withFile { file ->
        stream(file, { true }).use { input ->
            assertArrayEquals(file.readBytes(), input.readBytes())
            assertEquals(-1, input.read())
            assertEquals(0, input.read(ByteArray(0)))
        }
    }
    @Test fun stoppedStreamFailsInsteadOfReadingStaleData() = withFile { file ->
        stream(file, { true }, active = { false }).use { input ->
            assertThrows(IOException::class.java) { input.read() }
        }
        stream(file, { true }).also { it.close() }.use { input ->
            assertThrows(IOException::class.java) { input.read() }
        }
    }
    @Test fun waitsForPieceThenResumes() = withFile { file ->
        val ready = java.util.concurrent.atomic.AtomicBoolean(false)
        val requested = CountDownLatch(1)
        val executor = Executors.newSingleThreadExecutor()
        try {
            stream(file, { ready.get() }, request = { requested.countDown() }, timeout = 2000).use { input ->
                val result = executor.submit<Int> { input.read() }
                assertTrue(requested.await(1, TimeUnit.SECONDS))
                assertFalse(result.isDone)
                ready.set(true)
                assertEquals(1, result.get(2, TimeUnit.SECONDS))
            }
        } finally { executor.shutdownNow() }
    }
    @Test fun cancellationUnblocksPendingRead() = withFile { file ->
        val active = java.util.concurrent.atomic.AtomicBoolean(true)
        val requested = CountDownLatch(1)
        val executor = Executors.newSingleThreadExecutor()
        try {
            stream(file, { false }, { active.get() }, { requested.countDown() }, 2000).use { input ->
                val result = executor.submit<Boolean> {
                    try { input.read(); false } catch (e: IOException) { true }
                }
                assertTrue(requested.await(1, TimeUnit.SECONDS))
                active.set(false)
                assertTrue(result.get(1, TimeUnit.SECONDS))
            }
        } finally { executor.shutdownNow() }
    }
    @Test fun closeUnblocksPendingRead() = withFile { file ->
        val requested = CountDownLatch(1)
        val executor = Executors.newSingleThreadExecutor()
        try {
            stream(file, { false }, request = { requested.countDown() }, timeout = 2000).use { input ->
                val result = executor.submit<Boolean> {
                    try { input.read(); false } catch (e: IOException) { true }
                }
                assertTrue(requested.await(1, TimeUnit.SECONDS))
                input.close()
                assertTrue(result.get(1, TimeUnit.SECONDS))
            }
        } finally { executor.shutdownNow() }
    }
    @Test fun seekCancelsOldWaitAndReadsExactNewRange() = withFile { file ->
        val tracker = SeekRequestTracker()
        val oldGeneration = tracker.acceptRequest()
        val requested = CountDownLatch(1)
        val executor = Executors.newSingleThreadExecutor()
        try {
            stream(file, { false }, { tracker.isCurrent(oldGeneration) },
                { requested.countDown() }, 2000).use { old ->
                val oldResult = executor.submit<Boolean> {
                    try { old.read(); false } catch (_: IOException) { true }
                }
                assertTrue(requested.await(1, TimeUnit.SECONDS))
                tracker.beginSeek()
                val current = tracker.acceptRequest()
                VerifiedTorrentInputStream(file, 6, 8, { it >= 6 },
                    { fail("Already downloaded range must not be requested again") },
                    { tracker.isCurrent(current) },
                    { pos, count -> TorrentPlayback.readSize(pos, count, 8, 2, 4) }).use {
                    assertArrayEquals(byteArrayOf(7, 8), it.readBytes())
                }
                assertTrue(oldResult.get(1, TimeUnit.SECONDS))
            }
        } finally { executor.shutdownNow() }
    }
}
