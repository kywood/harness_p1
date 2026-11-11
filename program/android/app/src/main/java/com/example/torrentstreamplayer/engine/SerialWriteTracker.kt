package com.example.torrentstreamplayer.engine

import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException

/**
 * Runs one owner's disk writes on an executor shared by every owner, and tracks the owner's
 * pending writes so [close] / [awaitClosed] / [isClosed] keep per-owner shutdown semantics
 * without a thread per owner.
 */
internal class SerialWriteTracker(private val executor: Executor = SHARED) : Executor {
    private val lock = Object()
    private var closed = false
    private var pending = 0

    /** Queues [task]. After [close], throws [RejectedExecutionException] like a shut-down executor. */
    override fun execute(task: Runnable) {
        synchronized(lock) {
            if (closed) throw RejectedExecutionException("Writer is closed")
            pending++
        }
        try {
            executor.execute {
                try { task.run() } finally { finishOne() }
            }
        } catch (e: RejectedExecutionException) {
            finishOne()
            throw e
        }
    }

    private fun finishOne() = synchronized(lock) {
        pending--
        lock.notifyAll()
    }

    /** Stops accepting writes. Writes already queued still run. */
    fun close() = synchronized(lock) {
        closed = true
        lock.notifyAll()
    }

    /** True once [close] was called and every queued write has finished. */
    val isClosed: Boolean get() = synchronized(lock) { closed && pending == 0 }

    /** Waits up to [timeoutMs] for [isClosed]. Returns whether it became closed. */
    @Throws(InterruptedException::class)
    fun awaitClosed(timeoutMs: Long): Boolean = synchronized(lock) {
        val deadline = System.nanoTime() + timeoutMs * 1_000_000
        while (!(closed && pending == 0)) {
            val remainingMs = (deadline - System.nanoTime()) / 1_000_000
            if (remainingMs <= 0) return false
            lock.wait(remainingMs)
        }
        true
    }

    companion object {
        /** One app-wide daemon thread for checkpoint writes; it lives as long as the process. */
        private val SHARED: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "torrent-checkpoint-writer").apply { isDaemon = true }
        }
    }
}
