package com.example.torrentstreamplayer.ui
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
class DownloadControllerTest {
    @Test fun countsConnectingOrStalledDownloadAsActive() {
        assertEquals(1, DownloadTaskState(key = "a".repeat(64), speed = 0).activeCount)
    }
    @Test fun completedStoppedOrFailedTasksAreNotActive() {
        assertEquals(0, DownloadTaskState().activeCount)
        assertEquals(0, DownloadTaskState(key = "a".repeat(64), complete = true).activeCount)
        assertEquals(0, DownloadTaskState(key = "a".repeat(64), error = "failed").activeCount)
    }
    @Test fun idleAndRepeatedStopsNeverCreateNetworkEngine() {
        val scope = CoroutineScope(SupervisorJob())
        val task = DownloadController(scope, { error("Engine must stay lazy") }, {}, { _, _ -> })
        assertEquals(DownloadTaskState(), task.state.value)
        repeat(3) { task.stop() }
        assertEquals(DownloadTaskState(), task.state.value)
        scope.cancel()
    }
    @Test fun cancelledPendingDownloadCannotStartAfterStop() {
        val dispatcher = object : CoroutineDispatcher() {
            val pending = mutableListOf<Runnable>()
            override fun dispatch(context: kotlin.coroutines.CoroutineContext, block: Runnable) { pending += block }
        }
        val scope = CoroutineScope(SupervisorJob() + dispatcher)
        val task = DownloadController(scope, { error("Cancelled task must not create engine") }, {}, { _, _ -> })
        task.start("magnet:?xt=urn:btih:" + "1".repeat(40)) { true }
        task.stop()
        dispatcher.pending.toList().forEach { it.run() }
        assertEquals(DownloadTaskState(), task.state.value)
        scope.cancel()
    }
}
