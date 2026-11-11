package com.example.torrentstreamplayer.engine

import com.example.torrentstreamplayer.meta.EngineSpec
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Runs an engine's blocking shutdown steps (resume save, handle removal, session release)
 * off the caller thread so that stopping a download never blocks the main thread.
 *
 * Teardowns from every engine share one background thread and run in submission order.
 * Before an engine reacquires native resources it calls [await] so that a previous teardown
 * cannot release resources that the new run is already using.
 */
internal class EngineTeardown(
    private val onError: (Throwable) -> Unit,
    private val executor: ExecutorService = SHARED_EXECUTOR,
) {
    @Volatile private var pending: Future<*>? = null

    fun submit(steps: () -> Unit) {
        pending = executor.submit { runCatching(steps).onFailure(onError) }
    }

    /** Returns true when no teardown is pending or it finished within [timeoutMs]. */
    fun await(timeoutMs: Long = EngineSpec.TEARDOWN_AWAIT_MS): Boolean {
        val current = pending ?: return true
        return try {
            current.get(timeoutMs, TimeUnit.MILLISECONDS)
            true
        } catch (_: TimeoutException) {
            false
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            false
        }
    }

    private companion object {
        val SHARED_EXECUTOR: ExecutorService = Executors.newSingleThreadExecutor { task ->
            Thread(task, "torrent-teardown").apply { isDaemon = true }
        }
    }
}
