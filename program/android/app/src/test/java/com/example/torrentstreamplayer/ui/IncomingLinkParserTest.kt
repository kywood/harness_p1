package com.example.torrentstreamplayer.ui

import com.example.torrentstreamplayer.engine.MagnetIdentity
import com.example.torrentstreamplayer.meta.NetworkSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IncomingLinkParserTest {
    private val magnet = "magnet:?xt=urn:btih:" + "a".repeat(40) + "&dn=Big+Movie"
    private val view = IncomingAction.VIEW.action
    private val send = IncomingAction.SEND.action

    @Test fun actionNamesMatchAndroidIntentConstants() {
        assertEquals("android.intent.action.VIEW", view)
        assertEquals("android.intent.action.SEND", send)
    }

    @Test fun viewIntentGivesTheMagnetWithItsName() {
        val link = IncomingLinkParser.parse(view, magnet, null)!!
        assertEquals(magnet, link.value)
        assertEquals(IncomingLinkKind.MAGNET, link.kind)
        assertEquals("Big Movie", link.displayName)
        assertEquals(MagnetIdentity.key(magnet), link.magnetKey)
    }

    @Test fun otherActionsAndMissingPartsGiveNothing() {
        assertNull(IncomingLinkParser.parse("android.intent.action.MAIN", magnet, magnet))
        assertNull(IncomingLinkParser.parse(null, magnet, magnet))
        assertNull(IncomingLinkParser.parse(view, null, magnet))
        assertNull(IncomingLinkParser.parse(send, magnet, null))
    }

    @Test fun invalidMagnetsAreRejected() {
        assertNull(IncomingLinkParser.parse(view, "magnet:?xt=urn:btih:short", null))
        assertNull(IncomingLinkParser.parse(view, "magnet:?dn=only-a-name", null))
        val tooLong = magnet + "&tr=" + "x".repeat(NetworkSpec.MAX_LINK_CHARS)
        assertNull(IncomingLinkParser.parse(view, tooLong, null))
    }

    @Test fun sharedTextYieldsTheFirstValidLinkOnly() {
        val second = "magnet:?xt=urn:btih:" + "b".repeat(40)
        val text = "보세요 https://example.com/page 그리고 ($magnet). 또 $second"
        val link = IncomingLinkParser.parse(send, null, text)!!
        assertEquals("trailing ). is not part of the link", magnet, link.value)
    }

    @Test fun torrentAddressesNeedHttpAHostAndATorrentPath() {
        val link = IncomingLinkParser.parse(send, null, "file: https://example.com/files/Movie.TORRENT")!!
        assertEquals(IncomingLinkKind.TORRENT_URL, link.kind)
        assertNull(link.magnetKey)
        assertNull(link.displayName)
        assertNull(IncomingLinkParser.firstLink("https://example.com/download.php?id=3"))
        assertNull(IncomingLinkParser.link("ftp://example.com/a.torrent"))
        assertNull(IncomingLinkParser.link("http:///a.torrent"))
    }

    @Test fun hugeSharedTextIsNotScanned() {
        assertNull(IncomingLinkParser.firstLink(magnet + " " + "x".repeat(NetworkSpec.MAX_LINK_CHARS * 4)))
    }

    @Test fun policyRecommendsSavingDuringPlaybackAndDetectsPlaylistLinks() {
        val link = IncomingLinkParser.link(magnet)!!
        val state = IncomingLinkPolicy.state(link, setOf(MagnetIdentity.key(magnet)), playbackActive = true)
        assertEquals(PlaylistMembership.IN_PLAYLIST, state.membership)
        assertEquals(IncomingLinkAction.SAVE, state.recommended)
        assertEquals(PlaylistMembership.NEW, IncomingLinkPolicy.membership(link, emptySet()))
        assertEquals(IncomingLinkAction.PLAY_NOW, IncomingLinkPolicy.recommended(playbackActive = false))
        val url = IncomingLinkParser.link("https://example.com/a.torrent")!!
        assertEquals(PlaylistMembership.UNKNOWN, IncomingLinkPolicy.membership(url, setOf("x")))
    }

    @Test fun recreatedOrHistoryLaunchesDoNotHandleTheIntentAgain() {
        assertTrue(IncomingLinkPolicy.shouldHandle(recreated = false, launchedFromHistory = false))
        assertFalse(IncomingLinkPolicy.shouldHandle(recreated = true, launchedFromHistory = false))
        assertFalse(IncomingLinkPolicy.shouldHandle(recreated = false, launchedFromHistory = true))
    }
}
