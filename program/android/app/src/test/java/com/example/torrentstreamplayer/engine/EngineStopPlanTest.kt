package com.example.torrentstreamplayer.engine

import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class EngineStopPlanTest {
    private val executor = Executors.newSingleThreadExecutor()
    private val release = CountDownLatch(1)

    @After fun shutdown() {
        release.countDown()
        executor.shutdownNow()
    }

    private fun needsTeardown(
        started: Boolean = false,
        session: Boolean = false,
        torrent: Boolean = false,
        checkpoint: Boolean = false,
    ) = EngineStopPlan.needsTeardown(started, session, torrent, checkpoint)

    @Test fun engineThatNeverStartedHasNothingToTearDown() {
        assertFalse(needsTeardown())
    }

    @Test fun startedEngineIsTornDownEvenBeforeItHoldsASession() {
        assertTrue(needsTeardown(started = true))
    }

    @Test fun anyHeldResourceIsTornDown() {
        assertTrue(needsTeardown(session = true))
        assertTrue(needsTeardown(torrent = true))
        assertTrue(needsTeardown(checkpoint = true))
    }

    /** The stop flow of the engine: submit only when the plan says so, then start by awaiting the teardown. */
    @Test fun neverStartedEngineDoesNotWaitBehindAnotherEnginesSlowTeardown() {
        val stoppedEngine = EngineTeardown(onError = {}, executor = executor)
        stoppedEngine.submit { release.await(5, TimeUnit.SECONDS) }
        val newEngine = EngineTeardown(onError = {}, executor = executor)
        if (needsTeardown()) newEngine.submit {}
        assertTrue(newEngine.await(timeoutMs = 0))
    }

    @Test fun emptyTeardownWouldWaitBehindAnotherEnginesSlowTeardown() {
        val stoppedEngine = EngineTeardown(onError = {}, executor = executor)
        stoppedEngine.submit { release.await(5, TimeUnit.SECONDS) }
        val newEngine = EngineTeardown(onError = {}, executor = executor)
        if (needsTeardown(started = true)) newEngine.submit {}
        assertFalse(newEngine.await(timeoutMs = 50))
        release.countDown()
        assertTrue(newEngine.await(timeoutMs = 2_000))
    }
}
