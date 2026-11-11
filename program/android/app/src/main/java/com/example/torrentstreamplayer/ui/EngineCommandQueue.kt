package com.example.torrentstreamplayer.ui

import android.util.Log
import com.example.torrentstreamplayer.meta.EngineSpec
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Runs torrent engine calls that may block (engine lock, synchronous libtorrent queries, file reads)
 * on one background thread, in the order they were requested. UI code posts to this queue
 * instead of calling the engine directly, so a busy libtorrent session can never freeze a button.
 */
internal class EngineCommandQueue(
    private val executor: ExecutorService = SHARED_EXECUTOR,
    private val onError: (Throwable) -> Unit = { Log.e(TAG, "Engine command failed", it) },
) {
    /** Use with `withContext` when the caller needs a result; shares ordering with [post]. */
    val dispatcher: CoroutineDispatcher = executor.asCoroutineDispatcher()

    fun post(command: () -> Unit) {
        executor.execute { runCatching(command).onFailure(onError) }
    }

    /** Waits until every command posted before this call has run. Call only from a background thread. */
    fun awaitIdle(timeoutMs: Long = EngineSpec.TEARDOWN_AWAIT_MS): Boolean = try {
        executor.submit {}.get(timeoutMs, TimeUnit.MILLISECONDS)
        true
    } catch (_: TimeoutException) {
        false
    } catch (_: InterruptedException) {
        Thread.currentThread().interrupt()
        false
    }

    companion object {
        private const val TAG = "EngineCommandQueue"
        private val SHARED_EXECUTOR: ExecutorService = Executors.newSingleThreadExecutor { task ->
            Thread(task, "engine-commands").apply { isDaemon = true }
        }

        /** The app-wide queue; every screen and controller must use the same one to keep ordering. */
        val shared: EngineCommandQueue by lazy { EngineCommandQueue() }
    }
}
