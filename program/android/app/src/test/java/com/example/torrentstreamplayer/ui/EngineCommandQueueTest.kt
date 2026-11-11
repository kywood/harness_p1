package com.example.torrentstreamplayer.ui

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class EngineCommandQueueTest {
    private val executor = Executors.newSingleThreadExecutor()
    private val errors = CopyOnWriteArrayList<Throwable>()
    private val queue = EngineCommandQueue(executor) { errors += it }

    @After fun shutdown() { executor.shutdownNow() }

    @Test fun postReturnsImmediatelyWhileCommandBlocks() {
        val release = CountDownLatch(1)
        val started = System.nanoTime()
        queue.post { release.await(5, TimeUnit.SECONDS) }
        assertTrue((System.nanoTime() - started) / 1_000_000 < 200)
        assertFalse(queue.awaitIdle(timeoutMs = 50))
        release.countDown()
        assertTrue(queue.awaitIdle(timeoutMs = 2_000))
    }

    @Test fun postedCommandsAndDispatcherCallsShareOneOrder() = runBlocking {
        val order = CopyOnWriteArrayList<String>()
        queue.post { Thread.sleep(20); order += "seek" }
        val ready = withContext(queue.dispatcher) { order += "poll"; true }
        queue.post { order += "release" }
        assertTrue(queue.awaitIdle(timeoutMs = 2_000))
        assertTrue(ready)
        assertEquals(listOf("seek", "poll", "release"), order)
    }

    @Test fun failingCommandIsReportedAndQueueKeepsRunning() {
        var laterRan = false
        queue.post { error("engine busy") }
        queue.post { laterRan = true }
        assertTrue(queue.awaitIdle(timeoutMs = 2_000))
        assertTrue(laterRan)
        assertEquals(listOf("engine busy"), errors.map { it.message })
    }
}
