package com.example.torrentstreamplayer.ui

import com.example.torrentstreamplayer.ui.navigation.PlaybackOrigin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MobilePlaybackGateTest {
    private val first = "magnet:?xt=urn:btih:" + "a".repeat(40)
    private val second = "magnet:?xt=urn:btih:" + "b".repeat(40)

    @Test fun aHeldRequestIsAcceptedOnceOrForgotten() {
        val gate = MobilePlaybackGate()
        val request = MobilePlaybackRequest(first, PlaybackOrigin.PLAYLIST)
        gate.ask(request)
        assertEquals(request, gate.request.value)
        assertEquals(request, gate.accept())
        assertNull("accepted once", gate.accept())

        gate.ask(request)
        gate.clear()
        assertNull(gate.request.value)
        assertNull("a cleared request cannot be confirmed later", gate.accept())
    }

    /**
     * Review #135: while the dialog waits (stream idle), a link's "바로 재생" starts another playback. Every playback
     * start goes through MainViewModel.stopStream, which clears the gate; the stale request must not come back.
     */
    @Test fun linkPlaybackWhileTheDialogWaitsDropsTheOldRequest() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val gate = MobilePlaybackGate()
        val started = mutableListOf<String>()
        val playFromMain: (String) -> Unit = { link ->
            gate.clear() // stopStream() at the start of every playback
            started += link
        }
        val links = IncomingLinkCoordinator(scope, { null },{ false }, playFromMain, { })
        gate.ask(MobilePlaybackRequest(first, PlaybackOrigin.MAIN))

        links.receive(IncomingLinkParser.link(second))
        assertTrue(links.play())

        assertEquals(listOf(second), started)
        assertNull("the dialog closes", gate.request.value)
        assertNull("\"이번만 재생\" cannot reopen the earlier video", gate.accept())
        scope.cancel()
    }
}
