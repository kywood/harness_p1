package com.example.torrentstreamplayer.ui

import com.example.torrentstreamplayer.meta.NetworkSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ClipboardLinkPolicyTest {
    private val magnet = "magnet:?xt=urn:btih:" + "a".repeat(40)

    @Test fun emptyOrNonTextClipLeavesTheField() {
        assertNull(ClipboardLinkPolicy.pasteText(null))
        assertNull(ClipboardLinkPolicy.pasteText(""))
        assertNull(ClipboardLinkPolicy.pasteText("  \n "))
    }

    @Test fun linkIsTakenOutOfSurroundingText() {
        assertEquals(magnet, ClipboardLinkPolicy.pasteText("  $magnet \n"))
        assertEquals(magnet, ClipboardLinkPolicy.pasteText("이거 보세요 $magnet 끝."))
        assertEquals("https://example.com/a.torrent", ClipboardLinkPolicy.pasteText("HTTPS://example.com/a.torrent"))
    }

    @Test fun textWithoutALinkIsPastedTrimmedSoSubmitShowsTheError() {
        assertEquals("hello", ClipboardLinkPolicy.pasteText("  hello "))
        assertEquals("magnet:?xt=broken", ClipboardLinkPolicy.pasteText("magnet:?xt=broken"))
    }

    @Test fun overLongTextIsNotPasted() {
        assertNull(ClipboardLinkPolicy.pasteText("x".repeat(NetworkSpec.MAX_LINK_CHARS + 1)))
        assertNull(ClipboardLinkPolicy.pasteText("x".repeat(IncomingLinkParser.MAX_SHARED_CHARS * 3)))
        assertNull("a link cut by the length limit", ClipboardLinkPolicy.pasteText(
            "magnet:?xt=urn:btih:" + "a".repeat(40) + "&dn=" + "b".repeat(NetworkSpec.MAX_LINK_CHARS)))
    }

    @Test fun aLinkLaterInALongConversationIsStillFound() {
        val chat = "가".repeat(NetworkSpec.MAX_LINK_CHARS * 2) + " " + magnet
        assertEquals(magnet, ClipboardLinkPolicy.pasteText(chat))
    }
}
