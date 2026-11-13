package com.example.torrentstreamplayer.ui

import com.example.torrentstreamplayer.R
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
        coordinator.dismiss(coordinator.state.value!!)
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
        coordinator.dismiss(coordinator.state.value!!)
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
        assertEquals("clears only an input that still holds the saved link", first, coordinator.mainInput.value?.replaces)
        val event = coordinator.notice.value!!
        assertEquals(IncomingLinkNotice.SAVED, event.notice)
        assertEquals("nothing plays: the snackbar offers the playlist", R.string.action_open_playlist, event.action)
        coordinator.noticeShown(event)
        assertNull(coordinator.notice.value)
    }

    @Test fun aSuccessDuringPlaybackIsDroppedWhenRaisedEvenIfCollectedLater() = runBlocking {
        playing = true
        coordinator.receive(valid(first))
        coordinator.save()
        playing = false // playback ended before the screen collected the notice
        val event = coordinator.notice.value!!
        assertEquals(IncomingLinkNotice.SAVED, event.notice)
        assertEquals(NoticeTiming.DROP, event.timing)
        assertNull("leaving the player is not offered", event.action)
    }

    @Test fun aFailureDuringPlaybackWaitsAndStaysUntilConfirmed() = runBlocking {
        playing = true
        coordinator.receive(valid(first))
        saveFailure = IllegalStateException("x")
        coordinator.save()
        val event = coordinator.notice.value!!
        assertEquals(NoticeTiming.WAIT, event.timing)
        assertEquals(R.string.action_confirm, event.action)
    }

    @Test fun aNoticeRaisedWhileTheLastOneShowsIsKept() = runBlocking {
        coordinator.receive(valid(first))
        coordinator.save()
        val shown = coordinator.notice.value!!
        coordinator.receive(valid(second))
        coordinator.save()
        val newer = coordinator.notice.value!!
        coordinator.noticeShown(shown)
        assertEquals("two equal notices are two events", newer, coordinator.notice.value)
    }

    @Test fun cancellingWhileSavingStillClearsTheInputWhenTheSaveSucceeds() = runBlocking {
        coordinator.receive(valid(first))
        val gate = CompletableDeferred<Unit>()
        saveGate = gate
        val press = scope.launch { coordinator.save() }
        coordinator.dismiss(coordinator.state.value!!)
        assertNull(coordinator.state.value)
        gate.complete(Unit)
        press.join()
        assertEquals(listOf(first), saved)
        assertEquals("", coordinator.mainInput.value?.text)
    }

    @Test fun aClosingSheetDoesNotCloseANewerLink() {
        coordinator.receive(valid(first))
        val closing = coordinator.state.value!!
        coordinator.receive(valid(second))
        coordinator.dismiss(closing)
        assertEquals(second, confirm().link.value)
        coordinator.dismiss(coordinator.state.value!!)
        assertNull(coordinator.state.value)
    }

    @Test fun theInputIsFilledOnceAndNotAgainAfterItWasTaken() {
        coordinator.receive(valid(first))
        val fill = coordinator.mainInput.value!!
        coordinator.inputFilled(fill)
        assertNull("an erased link does not come back", coordinator.mainInput.value)
        coordinator.receive(valid(second))
        val newer = coordinator.mainInput.value!!
        coordinator.inputFilled(fill)
        assertEquals("an older fill does not drop a newer one", newer, coordinator.mainInput.value)
    }

    @Test fun aFailedSaveClosesTheSheetKeepsTheInputAndTellsTheFailure() = runBlocking {
        coordinator.receive(valid(first))
        saveFailure = IllegalArgumentException("토렌트 파일이 아닙니다.")
        coordinator.save()
        assertTrue(saved.isEmpty())
        assertNull(coordinator.state.value)
        assertEquals(first, coordinator.mainInput.value?.text)
        assertEquals(IncomingLinkNotice.SAVE_FAILED, coordinator.notice.value?.notice)
        assertNull(coordinator.notice.value?.action)
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
