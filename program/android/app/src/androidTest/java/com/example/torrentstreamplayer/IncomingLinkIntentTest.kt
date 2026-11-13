package com.example.torrentstreamplayer

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.torrentstreamplayer.ui.IncomingLinkAction
import com.example.torrentstreamplayer.ui.IncomingLinkError
import com.example.torrentstreamplayer.ui.IncomingLinkState
import com.example.torrentstreamplayer.ui.MainViewModel
import com.example.torrentstreamplayer.ui.StreamUiState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Links handed over by other apps (#127): a tapped magnet (VIEW), shared text (SEND), a link arriving while
 * something prepares (onNewIntent), and recreation not handling the same intent twice. Checks the ViewModel's
 * sheet state; the sheet itself only draws that state.
 *
 * Only the preparing case starts anything. Its magnet has no trackers, and the test deletes its playlist entry and
 * files through [MainViewModel.deleteSavedMagnet] (stop → awaitStoppedWrites → delete) so nothing is left behind.
 */
@RunWith(AndroidJUnit4::class)
class IncomingLinkIntentTest {
    private val magnet = "magnet:?xt=urn:btih:" + "c".repeat(40) + "&dn=Intent+Test"
    private val other = "magnet:?xt=urn:btih:" + "d".repeat(40)
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun viewIntent(link: String) = Intent(Intent.ACTION_VIEW, Uri.parse(link))
        .setClass(context, MainActivity::class.java)

    private fun sendIntent(text: CharSequence) = Intent(Intent.ACTION_SEND).setType("text/plain")
        .putExtra(Intent.EXTRA_TEXT, text).setClass(context, MainActivity::class.java)

    /** Runs [block] on the main thread with the activity's ViewModel and returns its result. */
    private fun <T> withModel(scenario: ActivityScenario<MainActivity>, block: (MainViewModel) -> T): T {
        var result: Result<T>? = null
        scenario.onActivity { result = runCatching { block(ViewModelProvider(it)[MainViewModel::class.java]) } }
        return result!!.getOrThrow()
    }

    /** Polls the sheet state for up to [timeoutMs] until [condition] holds. */
    private fun awaitState(scenario: ActivityScenario<MainActivity>, timeoutMs: Long = 5_000,
                           condition: (IncomingLinkState?) -> Boolean): IncomingLinkState? {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (true) {
            val state = withModel(scenario) { it.incomingLink.value }
            if (condition(state)) return state
            check(System.currentTimeMillis() < deadline) { "sheet state did not settle: $state" }
            Thread.sleep(50)
        }
    }

    private fun settledConfirm(scenario: ActivityScenario<MainActivity>) =
        awaitState(scenario) { it is IncomingLinkState.Confirm && !it.busy } as IncomingLinkState.Confirm

    private fun dismiss(scenario: ActivityScenario<MainActivity>) = withModel(scenario) { model ->
        model.incomingLink.value?.let(model::dismissIncomingLink)
    }

    @Test fun aTappedMagnetOpensTheConfirmSheetWithoutStartingAnything() {
        ActivityScenario.launch<MainActivity>(viewIntent(magnet)).use { scenario ->
            val state = settledConfirm(scenario)
            assertEquals(magnet, state.link.value)
            assertEquals(IncomingLinkAction.entries.toSet(), state.allowed)
            assertEquals(StreamUiState.Idle, withModel(scenario) { it.uiState.value })
            dismiss(scenario)
        }
    }

    @Test fun sharedTextOpensTheSheetForItsFirstLinkAndPlainHttpIsRejected() {
        val styled = android.text.SpannableString("이거 보세요\n$magnet\n그리고 $other")
        ActivityScenario.launch<MainActivity>(sendIntent(styled)).use { scenario ->
            assertEquals("styled shared text is read too", magnet, settledConfirm(scenario).link.value)
            dismiss(scenario)
        }
        ActivityScenario.launch<MainActivity>(sendIntent("http://example.com/a.torrent")).use { scenario ->
            val rejected = awaitState(scenario) { it is IncomingLinkState.Rejected }
            assertEquals(IncomingLinkState.Rejected(IncomingLinkError.HTTPS_REQUIRED), rejected)
            dismiss(scenario)
        }
    }

    @Test fun aLinkArrivingWhilePreparingOffersOnlySavingAndKeepsPlayback() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            var launchIntent: Intent? = null
            scenario.onActivity { launchIntent = it.intent }
            try {
                withModel(scenario) { it.playFromMain(other) }
                assertTrue(withModel(scenario) { it.uiState.value !is StreamUiState.Idle })
                // singleTask: a second hand-over reaches the running activity through onNewIntent.
                InstrumentationRegistry.getInstrumentation().targetContext
                    .startActivity(viewIntent(magnet).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                val state = settledConfirm(scenario)
                assertEquals(magnet, state.link.value)
                assertEquals(setOf(IncomingLinkAction.SAVE), state.allowed)
                assertTrue("playback goes on", withModel(scenario) { it.uiState.value !is StreamUiState.Idle })
                assertFalse(withModel(scenario) { it.playIncomingLink() })
                dismiss(scenario)
            } finally {
                withModel(scenario) { it.cancelToMain() }
                val model = withModel(scenario) { it }
                runBlocking { model.deleteSavedMagnet(other) }
                // onNewIntent calls setIntent; ActivityScenario ignores lifecycle events of an activity whose intent
                // no longer matches the one it launched, so close() would wait for DESTROYED forever.
                scenario.onActivity { it.intent = launchIntent }
            }
        }
    }

    @Test fun recreatingTheActivityDoesNotHandleTheSameLinkAgain() {
        ActivityScenario.launch<MainActivity>(viewIntent(magnet)).use { scenario ->
            settledConfirm(scenario)
            dismiss(scenario)
            scenario.recreate() // rotation and other configuration changes
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            Thread.sleep(500)
            assertNull(withModel(scenario) { it.incomingLink.value })
        }
    }
}
