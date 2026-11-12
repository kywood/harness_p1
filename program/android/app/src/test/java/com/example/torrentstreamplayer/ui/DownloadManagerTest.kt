package com.example.torrentstreamplayer.ui

import com.example.torrentstreamplayer.data.ConnectionType
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

    private fun manager(scope: CoroutineScope, created: MutableList<DownloadController> = mutableListOf()) =
        DownloadManager(scope, {
            DownloadController(scope, { error("Engine must stay lazy") }, {}, { _, _ -> }).also { created += it }
        })

    private fun phases(manager: DownloadManager) = manager.tasks.value.mapValues { it.value.phase }

    @Test fun wifiOnlyOnMobileDataStopsDownloadsAndWifiResumesThem() {
        val scope = CoroutineScope(SupervisorJob() + held)
        val created = mutableListOf<DownloadController>()
        val manager = manager(scope, created)
        manager.start(magnet('1')) { true }
        val key = manager.tasks.value.keys.single()
        assertEquals(DownloadPhase.RUNNING, phases(manager)[key])

        manager.applyNetwork(NetworkConditions(wifiOnly = true, connection = ConnectionType.MOBILE))
        assertEquals(DownloadPhase.WAITING_FOR_WIFI, phases(manager)[key])
        assertEquals("the stopped controller stays; no second engine is made", 1, created.size)

        manager.start(magnet('2')) { true }
        assertEquals("new downloads wait too", 2, phases(manager).values.count { it == DownloadPhase.WAITING_FOR_WIFI })

        manager.applyNetwork(NetworkConditions(wifiOnly = true, connection = ConnectionType.WIFI))
        assertEquals(listOf(DownloadPhase.RUNNING, DownloadPhase.RUNNING), phases(manager).values.toList())
        scope.cancel()
    }

    @Test fun allowOnceAndBorrowedPlaybackKeepRunningOnMobileData() {
        val scope = CoroutineScope(SupervisorJob() + held)
        val manager = manager(scope)
        manager.applyNetwork(NetworkConditions(wifiOnly = true, connection = ConnectionType.MOBILE))
        manager.start(magnet('1')) { true }
        manager.start(magnet('2')) { true }
        val (first, second) = manager.tasks.value.keys.toList()

        manager.allowOnce(first)
        assertEquals(DownloadPhase.RUNNING, phases(manager)[first])
        // borrow() hands out the engine last; the lazy test engine throws there, after the task is borrowed.
        runCatching { manager.borrow(second) }
        assertEquals(DownloadPhase.RUNNING, phases(manager)[second])

        manager.applyNetwork(NetworkConditions(wifiOnly = true, connection = ConnectionType.DATA_SAVER))
        assertEquals(listOf(DownloadPhase.RUNNING, DownloadPhase.RUNNING), phases(manager).values.toList())

        manager.release(second)
        assertEquals("after playback the loan ends", DownloadPhase.WAITING_FOR_WIFI, phases(manager)[second])
        manager.stop(first)
        manager.start(magnet('1')) { true }
        assertEquals("a stopped task forgets its permission", DownloadPhase.WAITING_FOR_WIFI, phases(manager)[first])
        scope.cancel()
    }
}
