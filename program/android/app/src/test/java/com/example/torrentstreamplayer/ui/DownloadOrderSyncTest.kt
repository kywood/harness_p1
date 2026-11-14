package com.example.torrentstreamplayer.ui

import com.example.torrentstreamplayer.engine.DownloadOrderPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class DownloadOrderSyncTest {
    private val key = "a".repeat(64)
    private val other = "b".repeat(64)

    @Test fun savesThenAppliesTheOrderOfEveryTask() = runBlocking {
        val saved = mutableMapOf<String, DownloadOrderPreference>()
        val applied = mutableListOf<Pair<String, DownloadOrderPreference>>()
        val sync = DownloadOrderSync(DownloadOrderLookup({ saved }, { DownloadOrderPreference.FROM_START }),
            Dispatchers.Unconfined) { task, order -> applied += task to order }
        sync.sync({ listOf(key, other) }, save = { saved[key] = DownloadOrderPreference.FASTEST })
        assertEquals(listOf(key to DownloadOrderPreference.FASTEST, other to DownloadOrderPreference.FROM_START), applied)
    }

    @Test fun aFailedSaveThrowsAndAppliesNothing() = runBlocking {
        val applied = mutableListOf<String>()
        val sync = DownloadOrderSync(DownloadOrderLookup({ emptyMap() }, { DownloadOrderPreference.FASTEST }),
            Dispatchers.Unconfined) { task, _ -> applied += task }
        val failure = runCatching { sync.sync({ listOf(key) }, save = { throw IOException("disk full") }) }
        assertTrue(failure.exceptionOrNull() is IOException)
        assertTrue(applied.isEmpty())
        sync.sync({ listOf(key) })
        assertEquals("the lock is released after a failure", listOf(key), applied)
    }

    /** A sync that read the old value and is slow to finish must not apply it after a later sync's new value. */
    @Test fun aSlowOlderSyncCannotOverwriteANewerOne() = runBlocking {
        val current = java.util.concurrent.atomic.AtomicReference(DownloadOrderPreference.FROM_START)
        val firstRead = AtomicBoolean(true)
        val reading = CountDownLatch(1)
        val release = CountDownLatch(1)
        val applied = CopyOnWriteArrayList<DownloadOrderPreference>()
        val lookup = DownloadOrderLookup({ emptyMap() }, {
            val value = current.get()
            if (firstRead.getAndSet(false)) {
                reading.countDown()
                release.await(5, TimeUnit.SECONDS)
            }
            value
        })
        val sync = DownloadOrderSync(lookup, Dispatchers.IO) { _, order -> applied += order }
        val older = launch(Dispatchers.Default) { sync.sync({ listOf(key) }) }
        assertTrue(reading.await(5, TimeUnit.SECONDS))
        current.set(DownloadOrderPreference.FASTEST)
        val newer = launch(Dispatchers.Default) { sync.sync({ listOf(key) }) }
        Thread.sleep(200) // Without the lock the newer sync would finish here, before the older one applies.
        release.countDown()
        older.join()
        newer.join()
        assertEquals(listOf(DownloadOrderPreference.FROM_START, DownloadOrderPreference.FASTEST), applied.toList())
    }
}
