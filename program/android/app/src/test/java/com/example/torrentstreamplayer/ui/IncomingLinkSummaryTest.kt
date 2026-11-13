package com.example.torrentstreamplayer.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IncomingLinkSummaryTest {
    private val hash = "3f2a9c1d" + "0".repeat(32)

    private fun link(value: String) = (IncomingLinkParser.link(value) as IncomingLinkResult.Valid).link

    @Test fun aMagnetWithANameShowsTheDecodedTrimmedName() {
        val summary = IncomingLinkSummary.of(link("magnet:?xt=urn:btih:$hash&dn=+Big.Buck.Bunny.2008.1080p.mkv+"))
        assertEquals(LinkSummary("Big.Buck.Bunny.2008.1080p.mkv", null, IncomingLinkKind.MAGNET), summary)
    }

    @Test fun aMagnetWithoutANameShowsTheFirstEightHashDigitsInUpperCase() {
        val summary = IncomingLinkSummary.of(link("magnet:?xt=urn:btih:$hash&tr=udp://t"))
        assertNull(summary.name)
        assertEquals("3F2A9C1D", summary.hashPrefix)
        assertFalse("the name line already says 마그넷 링크", summary.showKind)
    }

    @Test fun base32AndV2HashesGiveHexDigits() {
        // base32 of bytes 0x3f 0x2a 0x9c 0x1d followed by zero bytes.
        assertEquals("3F2A9C1D", IncomingLinkSummary.hashPrefix("magnet:?xt=urn:btih:H4VJYHIA" + "A".repeat(24)))
        assertEquals("ABCDEF01", IncomingLinkSummary.hashPrefix("magnet:?xt=urn:btmh:1220abcdef01" + "0".repeat(56)))
        assertNull(IncomingLinkSummary.hashPrefix("magnet:?dn=x"))
    }

    @Test fun anAddressShowsHostEllipsisAndLastStepWithoutTheQuery() {
        val summary = IncomingLinkSummary.of(link("https://example.org/releases/24.04/ubuntu-24.04.torrent?token=secret"))
        assertEquals(LinkSummary("example.org/…/ubuntu-24.04.torrent", null, IncomingLinkKind.TORRENT_URL), summary)
        assertEquals("example.org/a.torrent", IncomingLinkSummary.shortAddress("https://example.org/a.torrent?x=1"))
        assertEquals("example.org", IncomingLinkSummary.shortAddress("https://example.org/?id=3"))
    }

    @Test fun thePlaylistTitleWinsOverTheLinkName() {
        val magnet = link("magnet:?xt=urn:btih:$hash&dn=from-link")
        assertEquals("Saved.mkv", IncomingLinkSummary.of(magnet, "Saved.mkv").name)
        assertEquals("from-link", IncomingLinkSummary.of(magnet, " ").name)
    }

    @Test fun sheetButtonsFollowThePlaylistEntryAndPlayback() {
        val magnet = link("magnet:?xt=urn:btih:$hash")
        fun buttons(match: PlaylistMatch?, phase: DownloadPhase? = null, playing: Boolean = false, busy: Boolean = false) =
            IncomingLinkPolicy.buttons(IncomingLinkPolicy.state(magnet, match, playing).copy(busy = busy), phase)
        val partial = PlaylistMatch("m", "t", 0, complete = false)
        assertEquals(LinkSheetButtons(LinkSheetButton.PLAY_NOW, LinkSheetButton.SAVE, true), buttons(null))
        assertEquals(LinkSheetButton.RESUME, buttons(partial.copy(positionMs = 1)).primary)
        assertEquals("complete: nothing to save", null, buttons(partial.copy(complete = true)).secondary)
        assertEquals("already downloading", null, buttons(partial, DownloadPhase.RUNNING).secondary)
        assertEquals(null, buttons(partial, DownloadPhase.WAITING_FOR_WIFI).secondary)
        assertEquals("failed: save restarts it", LinkSheetButton.SAVE, buttons(partial, DownloadPhase.FAILED).secondary)
        assertEquals(LinkSheetButtons(LinkSheetButton.SAVE, null, true), buttons(null, playing = true))
        assertEquals(LinkSheetButtons(null, null, true), buttons(partial.copy(complete = true), playing = true))
        assertFalse(buttons(null, busy = true).enabled)
    }

    @Test fun aFailedSaveWaitsForTheSnackbarWhileASuccessIsDropped() {
        assertEquals(NoticeTiming.SHOW, IncomingLinkPolicy.noticeTiming(IncomingLinkNotice.SAVED, snackbarVisible = true))
        assertEquals(NoticeTiming.SHOW, IncomingLinkPolicy.noticeTiming(IncomingLinkNotice.SAVE_FAILED, true))
        assertEquals("the user must learn it was not saved",
            NoticeTiming.WAIT, IncomingLinkPolicy.noticeTiming(IncomingLinkNotice.SAVE_FAILED, snackbarVisible = false))
        assertEquals(NoticeTiming.DROP, IncomingLinkPolicy.noticeTiming(IncomingLinkNotice.SAVED, snackbarVisible = false))
    }

    @Test fun clearingAfterSavingAppliesOnlyToTheSavedLink() {
        val clear = MainInputFill("", 1, replaces = "magnet:?xt=a")
        assertTrue(clear.appliesTo(" magnet:?xt=a "))
        assertFalse("a link typed meanwhile stays", clear.appliesTo("magnet:?xt=b"))
        assertTrue("a received link always fills", MainInputFill("magnet:?xt=a", 2).appliesTo("anything"))
    }

    @Test fun aSaveWhileDownloadsWaitForWifiAnnouncesTheWait() {
        assertTrue(IncomingLinkPolicy.announcesWifiWait(IncomingLinkNotice.SAVED, waitsForWifi = true))
        assertFalse(IncomingLinkPolicy.announcesWifiWait(IncomingLinkNotice.SAVED, waitsForWifi = false))
        assertFalse("a failure says it failed", IncomingLinkPolicy.announcesWifiWait(IncomingLinkNotice.SAVE_FAILED, true))
    }
}
