package com.example.torrentstreamplayer.ui

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.concurrent.thread
import kotlin.coroutines.CoroutineContext

class DownloadManagerTest {
    /** Holds coroutines without running them, so no controller ever creates a network engine. */
    private val held = object : CoroutineDispatcher() {
        override fun dispatch(context: CoroutineContext, block: Runnable) = Unit
    }
    private fun magnet(digit: Char) = "magnet:?xt=urn:btih:" + digit.toString().repeat(40)

    @Test fun deleteWaitsOnASnapshotTakenBeforeLaterQueueChanges() {
        val scope = CoroutineScope(SupervisorJob() + held)
        val manager = DownloadManager(scope, {
            DownloadController(scope, { error("Engine must stay lazy") }, {}, { _, _ -> })
        })
        manager.start(magnet('1')) { true }
        manager.stop()
        val writers = manager.stoppedWriters()
        assertEquals(1, writers.size)

        manager.start(magnet('2')) { true }
        assertEquals(2, manager.stoppedWriters().size)
        thread { manager.awaitStoppedWrites(writers) }.join()

        assertEquals("the waited, stopped controller is forgotten; the new one stays", 1, manager.stoppedWriters().size)
        scope.cancel()
    }
}
