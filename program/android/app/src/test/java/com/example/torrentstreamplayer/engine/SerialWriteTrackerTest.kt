package com.example.torrentstreamplayer.engine

import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit

class SerialWriteTrackerTest {
    private class ManualExecutor : Executor {
        val queued = ArrayDeque<Runnable>()
        override fun execute(command: Runnable) { queued.addLast(command) }
        fun runAll() { while (queued.isNotEmpty()) queued.removeFirst().run() }
    }

    @Test fun closedOnlyAfterCloseAndQueuedWritesFinish() {
        val executor = ManualExecutor()
        val writer = SerialWriteTracker(executor)
        var written = 0
        writer.execute { written++ }
        assertFalse(writer.isClosed)
        writer.close()
        assertFalse(writer.isClosed)
        assertFalse(writer.awaitClosed(10))
        executor.runAll()
        assertEquals(1, written)
        assertTrue(writer.isClosed)
        assertTrue(writer.awaitClosed(10))
    }

    @Test fun writesAfterCloseAreRejected() {
        val executor = ManualExecutor()
        val writer = SerialWriteTracker(executor)
        writer.close()
        assertThrows(RejectedExecutionException::class.java) { writer.execute {} }
        assertTrue(executor.queued.isEmpty())
        assertTrue(writer.isClosed)
    }

    @Test fun notClosedWithoutCloseEvenWhenIdle() {
        val writer = SerialWriteTracker(ManualExecutor())
        assertFalse(writer.isClosed)
        assertFalse(writer.awaitClosed(10))
    }

    @Test fun failingWriteStillCountsAsFinished() {
        val executor = ManualExecutor()
        val writer = SerialWriteTracker(executor)
        writer.execute { throw IllegalStateException("disk full") }
        writer.close()
        assertThrows(IllegalStateException::class.java) { executor.runAll() }
        assertTrue(writer.isClosed)
    }

    @Test fun rejectingExecutorDoesNotLeavePendingWrite() {
        val writer = SerialWriteTracker { throw RejectedExecutionException() }
        assertThrows(RejectedExecutionException::class.java) { writer.execute {} }
        writer.close()
        assertTrue(writer.isClosed)
    }

    @Test fun sharedExecutorRunsWritesAndAwaitWaitsForThem() {
        val owners = List(3) { SerialWriteTracker() }
        val release = CountDownLatch(1)
        val done = CountDownLatch(owners.size)
        owners.forEach { writer -> writer.execute { release.await(5, TimeUnit.SECONDS); done.countDown() } }
        owners.forEach { it.close() }
        assertFalse(owners.first().isClosed)
        release.countDown()
        owners.forEach { assertTrue(it.awaitClosed(5_000)) }
        assertTrue(done.await(1, TimeUnit.SECONDS))
    }
}
