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

    private fun valid(result: IncomingLinkResult?): IncomingLink = (result as IncomingLinkResult.Valid).link
    private fun reason(result: IncomingLinkResult?): IncomingLinkError = (result as IncomingLinkResult.Invalid).reason
    private fun shared(text: String) = IncomingLinkParser.parse(send, null, text)

    @Test fun actionNamesMatchAndroidIntentConstants() {
        assertEquals("android.intent.action.VIEW", view)
        assertEquals("android.intent.action.SEND", send)
    }

    @Test fun viewIntentGivesTheMagnetWithItsName() {
        val link = valid(IncomingLinkParser.parse(view, " \t$magnet\n", null))
        assertEquals(magnet, link.value)
        assertEquals(IncomingLinkKind.MAGNET, link.kind)
        assertEquals("Big Movie", link.displayName)
        assertEquals(MagnetIdentity.key(magnet), link.magnetKey)
    }

    @Test fun viewIntentAcceptsOnlyMagnets() {
        val https = IncomingLinkParser.parse(view, "https://example.com/a.torrent", null)
        assertEquals(IncomingLinkError.UNSUPPORTED, reason(https))
        assertEquals(IncomingLinkError.UNSUPPORTED, reason(IncomingLinkParser.parse(view, null, magnet)))
    }

    @Test fun otherActionsAreNotHandOvers() {
        assertNull(IncomingLinkParser.parse("android.intent.action.MAIN", magnet, magnet))
        assertNull(IncomingLinkParser.parse(null, magnet, magnet))
    }

    @Test fun reasonsForLinksThatCannotBeOpened() {
        assertEquals(IncomingLinkError.UNSUPPORTED, reason(shared("링크가 없는 글")))
        assertEquals(IncomingLinkError.UNSUPPORTED, reason(IncomingLinkParser.parse(send, null, null)))
        assertEquals(IncomingLinkError.INVALID_MAGNET, reason(shared("magnet:?xt=urn:btih:short")))
        val nameOnly = IncomingLinkParser.parse(view, "magnet:?dn=only-a-name", null)
        assertEquals(IncomingLinkError.INVALID_MAGNET, reason(nameOnly))
        val tooLong = magnet + "&tr=" + "x".repeat(NetworkSpec.MAX_LINK_CHARS)
        assertEquals(IncomingLinkError.TOO_LONG, reason(IncomingLinkParser.parse(view, tooLong, null)))
        assertEquals(IncomingLinkError.HTTPS_REQUIRED, reason(shared("http://example.com/a.torrent")))
        assertEquals(IncomingLinkError.UNSUPPORTED, reason(shared("https:///a.torrent")))
    }

    @Test fun sharedTextYieldsOnlyTheFirstLink() {
        val second = "magnet:?xt=urn:btih:" + "b".repeat(40)
        assertEquals(magnet, valid(shared("보세요 ($magnet). 또 $second")).value)
        assertEquals("the first link decides, even when it fails",
            IncomingLinkError.HTTPS_REQUIRED, reason(shared("http://example.com/x $magnet")))
    }

    @Test fun httpsAddressesNeedNoTorrentSuffix() {
        val link = valid(shared("받기: https://example.com/download.php?id=3"))
        assertEquals(IncomingLinkKind.TORRENT_URL, link.kind)
        assertEquals("https://example.com/download.php?id=3", link.value)
        assertNull(link.magnetKey)
        assertNull(link.displayName)
        assertEquals(IncomingLinkKind.TORRENT_URL, valid(shared("https://Example.com/Movie.TORRENT")).kind)
    }

    @Test fun linksAcrossLinesTabsAndSpaces() {
        assertEquals(magnet, valid(shared("제목\n\n$magnet\n다음 줄")).value)
        assertEquals(magnet, valid(shared("\t  $magnet\t")).value)
        assertEquals("https://example.com/a.torrent", valid(shared("첫 줄\r\nhttps://example.com/a.torrent\r\n")).value)
    }

    @Test fun linksRightAfterOrBeforeHangulAreFound() {
        assertEquals(magnet, valid(shared("보세요$magnet")).value)
        assertEquals(magnet, valid(shared("${magnet}입니다")).value)
        assertEquals("letters before the scheme make it another word",
            IncomingLinkError.UNSUPPORTED, reason(shared("xmagnet:?xt=urn:btih:" + "a".repeat(40))))
    }

    @Test fun onlyTheFirstPartOfLongSharedTextIsSearched() {
        val filler = "가".repeat(20_000)
        assertEquals(magnet, valid(shared("$filler $magnet")).value)
        val beyond = "가".repeat(IncomingLinkParser.MAX_SHARED_CHARS)
        assertEquals(IncomingLinkError.UNSUPPORTED, reason(shared("$beyond $magnet")))
        assertEquals(magnet, valid(shared("$magnet $beyond")).value)
    }

    @Test fun policyAllowsOnlySavingDuringPlaybackAndDetectsPlaylistLinks() {
        val link = valid(IncomingLinkParser.link(magnet))
        val state = IncomingLinkPolicy.state(link, setOf(MagnetIdentity.key(magnet)), playbackActive = true)
        assertEquals(PlaylistMembership.IN_PLAYLIST, state.membership)
        assertEquals(setOf(IncomingLinkAction.SAVE), state.allowed)
        assertEquals(PlaylistMembership.NEW, IncomingLinkPolicy.membership(link, emptySet()))
        assertEquals(IncomingLinkAction.entries.toSet(), IncomingLinkPolicy.allowed(playbackActive = false))
        val url = valid(IncomingLinkParser.link("https://example.com/a.torrent"))
        assertEquals(PlaylistMembership.UNKNOWN, IncomingLinkPolicy.membership(url, setOf("x")))
    }

    @Test fun recreatedOrHistoryLaunchesDoNotHandleTheIntentAgain() {
        assertTrue(IncomingLinkPolicy.shouldHandle(recreated = false, launchedFromHistory = false))
        assertFalse(IncomingLinkPolicy.shouldHandle(recreated = true, launchedFromHistory = false))
        assertFalse(IncomingLinkPolicy.shouldHandle(recreated = false, launchedFromHistory = true))
    }
}
