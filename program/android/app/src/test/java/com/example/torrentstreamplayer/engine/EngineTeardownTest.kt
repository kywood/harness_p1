package com.example.torrentstreamplayer.engine

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class EngineTeardownTest {
    private val executor = Executors.newSingleThreadExecutor()
    private val errors = CopyOnWriteArrayList<Throwable>()
    private val teardown = EngineTeardown(onError = { errors += it }, executor = executor)

    @After fun shutdown() { executor.shutdownNow() }

    @Test fun submitReturnsWithoutWaitingForBlockingSteps() {
        val release = CountDownLatch(1)
        val started = System.nanoTime()
        teardown.submit { release.await(5, TimeUnit.SECONDS) }
        val elapsedMs = (System.nanoTime() - started) / 1_000_000
        assertTrue("submit blocked for $elapsedMs ms", elapsedMs < 200)
        assertFalse(teardown.await(timeoutMs = 50))
        release.countDown()
        assertTrue(teardown.await(timeoutMs = 2_000))
    }

    @Test fun teardownsRunInSubmissionOrder() {
        val order = CopyOnWriteArrayList<Int>()
        repeat(5) { index -> teardown.submit { Thread.sleep(5); order += index } }
        assertTrue(teardown.await(timeoutMs = 2_000))
        assertEquals(listOf(0, 1, 2, 3, 4), order)
    }

    @Test fun failedTeardownIsReportedAndLaterTeardownStillRuns() {
        var laterRan = false
        teardown.submit { error("native remove failed") }
        teardown.submit { laterRan = true }
        assertTrue(teardown.await(timeoutMs = 2_000))
        assertTrue(laterRan)
        assertEquals(listOf("native remove failed"), errors.map { it.message })
    }

    @Test fun awaitWithoutPendingTeardownReturnsImmediately() {
        assertTrue(teardown.await(timeoutMs = 0))
    }
}
