package com.example.torrentstreamplayer.engine

import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class SameKeyTeardownsTest {
    private val executor = Executors.newSingleThreadExecutor()
    private val release = CountDownLatch(1)
    private val sameKey = SameKeyTeardowns()

    @After fun shutdown() {
        release.countDown()
        executor.shutdownNow()
    }

    private fun stoppedEngine(key: String): EngineTeardown {
        val teardown = EngineTeardown(onError = {}, executor = executor)
        teardown.submit { release.await(5, TimeUnit.SECONDS) }
        sameKey.stopped(key, teardown)
        return teardown
    }

    @Test fun keyThatNoEngineStoppedDoesNotWait() {
        assertTrue(sameKey.await("a", timeoutMs = 0))
    }

    @Test fun sameKeyWaitsForTheStoppedEnginesTeardown() {
        stoppedEngine("a")
        assertFalse(sameKey.await("a", timeoutMs = 50))
        release.countDown()
        assertTrue(sameKey.await("a", timeoutMs = 2_000))
    }

    @Test fun otherKeyDoesNotWaitForARunningTeardown() {
        stoppedEngine("a")
        assertTrue(sameKey.await("b", timeoutMs = 0))
    }

    @Test fun latestStoppedEngineOfAKeyIsTheOneAwaited() {
        val first = stoppedEngine("a")
        release.countDown()
        assertTrue(first.await(timeoutMs = 2_000))
        val blocked = CountDownLatch(1)
        val second = EngineTeardown(onError = {}, executor = executor)
        second.submit { blocked.await(5, TimeUnit.SECONDS) }
        sameKey.stopped("a", second)
        assertFalse(sameKey.await("a", timeoutMs = 50))
        blocked.countDown()
        assertTrue(sameKey.await("a", timeoutMs = 2_000))
    }
}
