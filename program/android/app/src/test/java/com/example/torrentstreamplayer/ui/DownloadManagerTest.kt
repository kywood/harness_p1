package com.example.torrentstreamplayer.ui

import com.example.torrentstreamplayer.data.ConnectionType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import com.example.torrentstreamplayer.engine.MagnetIdentity
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

    @Test fun borrowedPlaybackAndShortDropsAreLeftAlone() {
        val scope = CoroutineScope(SupervisorJob() + held)
        val manager = manager(scope)
        manager.applyNetwork(NetworkConditions(wifiOnly = true, connection = ConnectionType.WIFI))
        manager.start(magnet('1')) { true }
        manager.start(magnet('2')) { true }
        val (playing, other) = manager.tasks.value.keys.toList()
        runCatching { manager.borrow(playing) }

        manager.applyNetwork(NetworkConditions(wifiOnly = true, connection = ConnectionType.NONE))
        assertEquals("no connection changes nothing", listOf(DownloadPhase.RUNNING, DownloadPhase.RUNNING),
            phases(manager).values.toList())
        manager.applyNetwork(NetworkConditions(wifiOnly = true, connection = ConnectionType.MOBILE))
        assertEquals("playback keeps going on mobile data", DownloadPhase.RUNNING, phases(manager)[playing])
        assertEquals(DownloadPhase.WAITING_FOR_WIFI, phases(manager)[other])
        manager.applyNetwork(NetworkConditions(wifiOnly = true, connection = ConnectionType.NONE))
        assertEquals("a drop after mobile keeps waiting", DownloadPhase.WAITING_FOR_WIFI, phases(manager)[other])
        manager.start(magnet('2')) { true }
        assertEquals("starting a waiting task again does not replace it", DownloadPhase.WAITING_FOR_WIFI,
            phases(manager)[other])
        scope.cancel()
    }

    /** Review #135 case: Wi-Fi only on, watching on Wi-Fi, Wi-Fi drops. Playback must not stop for Wi-Fi. */
    @Test fun wifiDropWhileWatchingKeepsPlaybackAndReportsMobileData() {
        val scope = CoroutineScope(SupervisorJob() + held)
        val manager = manager(scope)
        manager.applyNetwork(NetworkConditions(wifiOnly = true, connection = ConnectionType.WIFI))
        manager.start(magnet('1')) { true }
        val key = manager.tasks.value.keys.single()
        runCatching { manager.borrow(key) }

        manager.applyNetwork(NetworkConditions(wifiOnly = true, connection = ConnectionType.NONE))
        assertEquals("Wi-Fi lost: no connection yet", DownloadPhase.RUNNING, phases(manager)[key])
        assertNull(manager.mobilePlayback.value)
        manager.applyNetwork(NetworkConditions(wifiOnly = true, connection = ConnectionType.MOBILE))
        assertEquals("playback goes on over mobile data", DownloadPhase.RUNNING, phases(manager)[key])
        assertEquals(key, manager.mobilePlayback.value)
        manager.applyNetwork(NetworkConditions(wifiOnly = true, connection = ConnectionType.WIFI))
        assertNull("back on Wi-Fi", manager.mobilePlayback.value)
        manager.applyNetwork(NetworkConditions(wifiOnly = true, connection = ConnectionType.MOBILE))
        manager.release(key)
        assertNull("the notice ends with playback", manager.mobilePlayback.value)
        assertEquals(DownloadPhase.WAITING_FOR_WIFI, phases(manager)[key])
        scope.cancel()
    }

    @Test fun waitingTaskIsNotLentToPlaybackWithoutPermission() {
        val scope = CoroutineScope(SupervisorJob() + held)
        val manager = manager(scope)
        manager.applyNetwork(NetworkConditions(wifiOnly = true, connection = ConnectionType.MOBILE))
        manager.start(magnet('1')) { true }
        val key = manager.tasks.value.keys.single()

        assertEquals(PlaybackNetworkCheck.CONFIRM_MOBILE,
            manager.preparePlayback(key, complete = false, allowMobileOnce = false))
        assertNull("not started on mobile data", manager.borrow(key))
        assertEquals(DownloadPhase.WAITING_FOR_WIFI, phases(manager)[key])

        assertEquals(PlaybackNetworkCheck.START, manager.preparePlayback(key, complete = false, allowMobileOnce = true))
        assertEquals(DownloadPhase.RUNNING, phases(manager)[key])
        assertEquals("allowed once stays allowed", PlaybackNetworkCheck.START,
            manager.preparePlayback(key, complete = false, allowMobileOnce = false))
        assertNull("allowed playback on mobile data is not reported", manager.mobilePlayback.value)
        scope.cancel()
    }

    /** Review #135 (2nd): Wi-Fi only on mobile data, playing a finished file must not fail at borrow. */
    @Test fun finishedFileOnMobileDataIsLentWithoutAsking() {
        val scope = CoroutineScope(SupervisorJob() + held)
        val manager = manager(scope)
        manager.applyNetwork(NetworkConditions(wifiOnly = true, connection = ConnectionType.MOBILE))
        val key = MagnetIdentity.key(magnet('1'))

        // The ViewModel's order: decide, then start the task, then borrow it.
        assertEquals(PlaybackNetworkCheck.START, manager.preparePlayback(key, complete = true, allowMobileOnce = false))
        manager.start(magnet('1')) { true }
        assertEquals(DownloadPhase.RUNNING, phases(manager)[key])
        // A refusal returns null; a loan reaches the engine, where the lazy test engine throws.
        val lent = runCatching { manager.borrow(key) }
        assertTrue("borrow does not refuse it", lent.isFailure)
        assertNull("a finished file uses no data", manager.mobilePlayback.value)
        scope.cancel()
    }

    @Test fun phasesRegisterWhatIsAlive() {
        assertEquals(setOf(DownloadPhase.QUEUED, DownloadPhase.RUNNING, DownloadPhase.WAITING_FOR_WIFI),
            DownloadPhase.entries.filter { it.stoppable }.toSet())
        assertEquals(setOf(DownloadPhase.QUEUED, DownloadPhase.RUNNING),
            DownloadPhase.entries.filter { it.networkGated }.toSet())
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
        runCatching { manager.borrow(first) }
        assertEquals(DownloadPhase.RUNNING, phases(manager)[first])
        assertNull("a waiting task is not lent without permission", manager.borrow(second))

        manager.applyNetwork(NetworkConditions(wifiOnly = true, connection = ConnectionType.DATA_SAVER))
        assertEquals(listOf(DownloadPhase.RUNNING, DownloadPhase.WAITING_FOR_WIFI), phases(manager).values.toList())

        manager.release(first)
        assertEquals("the one-time permission outlives the loan", DownloadPhase.RUNNING, phases(manager)[first])
        manager.stop(first)
        manager.start(magnet('1')) { true }
        assertEquals("a stopped task forgets its permission", DownloadPhase.WAITING_FOR_WIFI, phases(manager)[first])
        scope.cancel()
    }

    @Test fun queuedAndWifiWaitingTasksReachThePlaylistBeforeTheyRun() {
        val scope = CoroutineScope(SupervisorJob() + held)
        val added = mutableListOf<String>()
        val manager = DownloadManager(scope, {
            DownloadController(scope, { error("Engine must stay lazy") }, {}, { _, _ -> })
        }, limit = 3, onTaskAdded = { added += it })
        val magnets = listOf('1', '2', '3', '4').map(::magnet)
        magnets.forEach { manager.start(it) { true } }

        val fourth = MagnetIdentity.key(magnets.last())
        assertEquals(DownloadPhase.QUEUED, phases(manager)[fourth])
        assertEquals("only the queued 4th link is reported; running ones are saved by their controllers",
            listOf(magnets.last()), added)
        val queued = PlaylistItemStatus.of(manager.entry(fourth), playing = false, complete = false)
        assertEquals(PlaylistItemStatus.QUEUED, queued)

        manager.applyNetwork(NetworkConditions(wifiOnly = true, connection = ConnectionType.MOBILE))
        manager.start(magnet('5')) { true }
        assertEquals(magnet('5'), added.last())
        val waiting = PlaylistItemStatus.of(manager.entry(MagnetIdentity.key(magnet('5'))), playing = false,
            complete = false)
        assertEquals("Wi-Fi 대기 wins over 다운로드 대기", PlaylistItemStatus.WAITING_FOR_WIFI, waiting)
        scope.cancel()
    }

    @Test fun eachWaitingTaskIsReportedOnce() {
        val scope = CoroutineScope(SupervisorJob() + held)
        val added = mutableListOf<String>()
        val manager = DownloadManager(scope, {
            DownloadController(scope, { error("Engine must stay lazy") }, {}, { _, _ -> })
        }, limit = 1, onTaskAdded = { added += it })
        manager.start(magnet('1')) { true }
        assertEquals("a task that runs at once is not reported", emptyList<String>(), added)
        manager.start(magnet('2')) { true }
        manager.start(magnet('2')) { true }
        assertEquals("starting a waiting task again reports nothing more", listOf(magnet('2')), added)
        scope.cancel()
    }
}
