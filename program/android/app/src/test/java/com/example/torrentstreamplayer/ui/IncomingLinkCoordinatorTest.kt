package com.example.torrentstreamplayer.ui

import com.example.torrentstreamplayer.engine.MagnetIdentity
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class IncomingLinkCoordinatorTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val first = "magnet:?xt=urn:btih:" + "a".repeat(40)
    private val second = "magnet:?xt=urn:btih:" + "b".repeat(40)
    private var playing = false
    private var keys: suspend () -> Set<String> = { emptySet() }
    private val played = mutableListOf<String>()
    private val saved = mutableListOf<String>()
    private var saveFailure: Exception? = null

    private val coordinator = IncomingLinkCoordinator(scope, { keys() }, { playing }, { played += it }, { link ->
        saveFailure?.let { throw it }
        saved += link
    })

    private fun valid(value: String) = IncomingLinkParser.link(value)

    @After fun tearDown() = scope.cancel()

    @Test fun validLinkWaitsForConfirmationWithPlaylistMembership() {
        keys = { setOf(MagnetIdentity.key(first)) }
        coordinator.receive(valid(first))
        val state = coordinator.state.value as IncomingLinkState.Confirm
        assertEquals(first, state.link.value)
        assertEquals(PlaylistMembership.IN_PLAYLIST, state.membership)
        assertEquals(IncomingLinkAction.entries.toSet(), state.allowed)
        assertTrue("nothing starts on its own", played.isEmpty() && saved.isEmpty())
    }

    @Test fun invalidLinkShowsTheReason() {
        coordinator.receive(IncomingLinkParser.firstLink("링크 없음"))
        assertEquals(IncomingLinkState.Rejected(IncomingLinkError.UNSUPPORTED), coordinator.state.value)
        assertFalse(coordinator.play())
        coordinator.dismiss()
        assertNull(coordinator.state.value)
    }

    @Test fun aSlowPlaylistReadNeverOverwritesANewerLink() {
        val slow = CompletableDeferred<Set<String>>()
        keys = { slow.await() }
        coordinator.receive(valid(first))
        keys = { emptySet() }
        coordinator.receive(valid(second))
        slow.complete(emptySet())
        assertEquals(second, (coordinator.state.value as IncomingLinkState.Confirm).link.value)
    }

    @Test fun dismissingBeforeThePlaylistReadKeepsTheSheetClosed() {
        val slow = CompletableDeferred<Set<String>>()
        keys = { slow.await() }
        coordinator.receive(valid(first))
        coordinator.dismiss()
        slow.complete(emptySet())
        assertNull(coordinator.state.value)
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

        playing = true
        coordinator.receive(valid(second))
        assertEquals(setOf(IncomingLinkAction.SAVE), (coordinator.state.value as IncomingLinkState.Confirm).allowed)
    }

    @Test fun saveAddsTheLinkAndAFailureKeepsTheSheet() = runBlocking {
        coordinator.receive(valid(first))
        saveFailure = IllegalArgumentException("토렌트 파일이 아닙니다.")
        try {
            coordinator.save()
            fail("save must rethrow")
        } catch (expected: IllegalArgumentException) {
            assertTrue(coordinator.state.value is IncomingLinkState.Confirm)
        }
        saveFailure = null
        coordinator.save()
        assertEquals(listOf(first), saved)
        assertNull(coordinator.state.value)
    }
}
