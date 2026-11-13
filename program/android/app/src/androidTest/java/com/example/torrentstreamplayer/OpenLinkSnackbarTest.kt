package com.example.torrentstreamplayer

import androidx.annotation.StringRes
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelProvider
import com.example.torrentstreamplayer.ui.IncomingLinkParser
import com.example.torrentstreamplayer.ui.MainViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

/**
 * The snackbar after "받아 두기" stays readable (#141 H1: it used to vanish in a frame). The address uses the reserved
 * `.invalid` domain, so the save fails at name lookup: no torrent traffic and no playlist entry is left behind.
 */
@RunWith(AndroidJUnit4::class)
class OpenLinkSnackbarTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun text(@StringRes id: Int) = compose.activity.getString(id)

    @Test fun theSaveResultSnackbarStaysOnScreen() {
        compose.runOnUiThread {
            ViewModelProvider(compose.activity)[MainViewModel::class.java]
                .receiveLink(IncomingLinkParser.link("https://pieceplay-test.invalid/a.torrent"))
        }
        compose.waitUntil(5_000) {
            compose.onAllNodes(hasText(text(R.string.open_link_title))).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText(text(R.string.action_save_for_later), useUnmergedTree = true).performClick()
        val message = text(R.string.error_download_start)
        compose.waitUntil(30_000) { compose.onAllNodes(hasText(message)).fetchSemanticsNodes().isNotEmpty() }
        Thread.sleep(1_500)
        compose.onNodeWithText(message).assertExists("the snackbar must not be cancelled right after it appears")
    }
}
