package com.example.torrentstreamplayer.ui

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IncomingLinkCoordinatorTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val first = "magnet:?xt=urn:btih:" + "a".repeat(40)
    private val second = "magnet:?xt=urn:btih:" + "b".repeat(40)
    private var playing = false
    private var lookup: suspend (String) -> PlaylistMatch? = { null }
    private val played = mutableListOf<String>()
    private val saved = mutableListOf<String>()
    private var saveFailure: Exception? = null
    private var saveGate: CompletableDeferred<Unit>? = null

    private val coordinator = IncomingLinkCoordinator(scope, { lookup(it) }, { playing }, { played += it }, { link ->
        saveGate?.await()
        saveFailure?.let { throw it }
        saved += link
    })

    private fun valid(value: String) = IncomingLinkParser.link(value)
    private fun confirm() = coordinator.state.value as IncomingLinkState.Confirm

    @After fun tearDown() = scope.cancel()

    @Test fun validLinkWaitsForConfirmationWithItsPlaylistEntry() {
        val match = PlaylistMatch(first, "Movie.mkv", 5_000, complete = false)
        lookup = { match }
        coordinator.receive(valid(first))
        val state = confirm()
        assertEquals(first, state.link.value)
        assertEquals(PlaylistMembership.IN_PLAYLIST, state.membership)
        assertEquals(match, state.match)
        assertFalse(state.busy)
        assertEquals(IncomingLinkAction.entries.toSet(), state.allowed)
        assertTrue("nothing starts on its own", played.isEmpty() && saved.isEmpty())
        assertEquals("the main input takes the link", first, coordinator.mainInput.value?.text)
    }

    @Test fun invalidLinkShowsTheReasonAndLeavesTheInputAlone() {
        coordinator.receive(IncomingLinkParser.firstLink("링크 없음"))
        assertEquals(IncomingLinkState.Rejected(IncomingLinkError.UNSUPPORTED), coordinator.state.value)
        assertNull(coordinator.mainInput.value)
        assertFalse(coordinator.play())
        coordinator.dismiss()
        assertNull(coordinator.state.value)
    }

    @Test fun aFailedPlaylistReadCountsAsNotInThePlaylist() {
        lookup = { error("disk") }
        coordinator.receive(valid(first))
        assertEquals(PlaylistMembership.NEW, confirm().membership)
        assertFalse(confirm().busy)
    }

    @Test fun aSlowPlaylistReadNeverOverwritesANewerLink() {
        val slow = CompletableDeferred<PlaylistMatch?>()
        lookup = { slow.await() }
        coordinator.receive(valid(first))
        lookup = { null }
        coordinator.receive(valid(second))
        slow.complete(PlaylistMatch(first, "", 0, complete = false))
        assertEquals(second, confirm().link.value)
        assertNull(confirm().match)
    }

    @Test fun whileTheNewLinkIsLookedUpTheSheetShowsItButCannotPlayOrSave() = runBlocking {
        coordinator.receive(valid(first))
        val slow = CompletableDeferred<PlaylistMatch?>()
        lookup = { slow.await() }
        coordinator.receive(valid(second))
        assertEquals("the older link is no longer on the sheet", second, confirm().link.value)
        assertTrue(confirm().busy)
        assertFalse(IncomingLinkPolicy.buttons(confirm(), null).enabled)
        assertFalse(coordinator.play())
        coordinator.save()
        assertTrue(played.isEmpty() && saved.isEmpty())
        slow.complete(null)
        assertTrue(coordinator.play())
        assertEquals(listOf(second), played)
    }

    @Test fun dismissingBeforeThePlaylistReadKeepsTheSheetClosed() {
        val slow = CompletableDeferred<PlaylistMatch?>()
        lookup = { slow.await() }
        coordinator.receive(valid(first))
        coordinator.dismiss()
        slow.complete(null)
        assertNull(coordinator.state.value)
        assertEquals("cancel keeps the link in the main input", first, coordinator.mainInput.value?.text)
    }

    @Test fun playStartsTheLinkAndClosesTheSheet() {
        coordinator.receive(valid(first))
        assertTrue(coordinator.play())
        assertEquals(listOf(first), played)
        assertNull(coordinator.state.value)
    }

    @Test fun playIsRefusedWhilePlaybackRunsEvenIfItStartedAfterTheSheetOpened() {
        coordinator.receive(valid(first))
        playing = true
        assertFalse(coordinator.play())
        assertTrue(played.isEmpty())
        assertTrue("the sheet stays for saving", coordinator.state.value is IncomingLinkState.Confirm)

        coordinator.receive(valid(second))
        assertEquals(setOf(IncomingLinkAction.SAVE), confirm().allowed)
    }

    @Test fun saveAddsTheLinkClosesTheSheetClearsTheInputAndTellsTheResult() = runBlocking {
        coordinator.receive(valid(first))
        coordinator.save()
        assertEquals(listOf(first), saved)
        assertNull(coordinator.state.value)
        assertEquals("", coordinator.mainInput.value?.text)
        assertEquals(IncomingLinkNotice.SAVED, coordinator.notice.value)
        coordinator.noticeShown()
        assertNull(coordinator.notice.value)
    }

    @Test fun aFailedSaveClosesTheSheetKeepsTheInputAndTellsTheFailure() = runBlocking {
        coordinator.receive(valid(first))
        saveFailure = IllegalArgumentException("토렌트 파일이 아닙니다.")
        coordinator.save()
        assertTrue(saved.isEmpty())
        assertNull(coordinator.state.value)
        assertEquals(first, coordinator.mainInput.value?.text)
        assertEquals(IncomingLinkNotice.SAVE_FAILED, coordinator.notice.value)
    }

    @Test fun pressingSaveTwiceSavesOnce() = runBlocking {
        coordinator.receive(valid(first))
        val gate = CompletableDeferred<Unit>()
        saveGate = gate
        val firstPress = scope.launch { coordinator.save() }
        assertTrue(confirm().busy)
        coordinator.save()
        gate.complete(Unit)
        firstPress.join()
        assertEquals(listOf(first), saved)
    }

    @Test fun aLinkThatArrivesDuringSavingStaysOnTheSheet() = runBlocking {
        coordinator.receive(valid(first))
        val gate = CompletableDeferred<Unit>()
        saveGate = gate
        val press = scope.launch { coordinator.save() }
        coordinator.receive(valid(second))
        gate.complete(Unit)
        press.join()
        assertEquals(second, confirm().link.value)
        assertEquals("the newer link stays in the main input", second, coordinator.mainInput.value?.text)
    }
}
