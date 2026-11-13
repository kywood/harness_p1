package com.example.torrentstreamplayer.ui

import com.example.torrentstreamplayer.R
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

    @Test fun linksRightAfterHangulAreFoundAndHttpsEndsAtHangul() {
        assertEquals(magnet, valid(shared("보세요$magnet")).value)
        assertEquals("https://example.com/a.torrent", valid(shared("https://example.com/a.torrent입니다")).value)
        assertEquals("letters before the scheme make it another word",
            IncomingLinkError.UNSUPPORTED, reason(shared("xmagnet:?xt=urn:btih:" + "a".repeat(40))))
    }

    @Test fun aMagnetEndsOnlyAtWhitespaceSoAnUnencodedHangulNameStaysWhole() {
        val hangul = "magnet:?xt=urn:btih:" + "a".repeat(40) + "&dn=영화 제목"
        val link = valid(shared("보세요 $hangul"))
        assertEquals("magnet:?xt=urn:btih:" + "a".repeat(40) + "&dn=영화", link.value)
        assertEquals("영화", link.displayName)
        val joined = "magnet:?xt=urn:btih:" + "a".repeat(40) + "&dn=영화제목.mkv"
        assertEquals("영화제목.mkv", valid(shared("링크　$joined 끝")).displayName)
    }

    @Test fun onlyTheFirstPartOfLongSharedTextIsSearched() {
        val filler = "가".repeat(20_000)
        assertEquals(magnet, valid(shared("$filler $magnet")).value)
        val beyond = "가".repeat(IncomingLinkParser.MAX_SHARED_CHARS)
        assertEquals(IncomingLinkError.UNSUPPORTED, reason(shared("$beyond $magnet")))
        assertEquals(magnet, valid(shared("$magnet $beyond")).value)
    }

    @Test fun aLinkCutAtTheEndOfTheSearchedPartIsDropped() {
        val address = "https://example.com/" + "a".repeat(100)
        val lead = "가".repeat(IncomingLinkParser.MAX_SHARED_CHARS - 50) + " "
        assertEquals(IncomingLinkError.TOO_LONG, reason(shared(lead + address)))
        val fits = "가".repeat(IncomingLinkParser.MAX_SHARED_CHARS - address.length - 1) + " "
        assertEquals("ending exactly at the boundary is whole", address, valid(shared(fits + address + " 끝")).value)
        assertEquals("Hangul ends an https address, so it was not cut", address, valid(shared(fits + address + "입니다")).value)
        val magnetLead = "가".repeat(IncomingLinkParser.MAX_SHARED_CHARS - magnet.length - 1) + " "
        assertEquals("Hangul continues a magnet's name, so it was cut",
            IncomingLinkError.TOO_LONG, reason(shared(magnetLead + magnet + "입니다")))
        assertEquals(magnet, valid(shared(magnetLead + magnet + "　끝")).value)
    }

    @Test fun playingKeepsTheSnackbarToText() {
        assertNull(IncomingLinkPolicy.noticeAction(IncomingLinkNotice.SAVED, NoticeTiming.DROP))
        assertEquals(IncomingLinkNotice.SAVED.action, IncomingLinkPolicy.noticeAction(IncomingLinkNotice.SAVED, NoticeTiming.SHOW))
        assertNull(IncomingLinkPolicy.noticeAction(IncomingLinkNotice.SAVE_FAILED, NoticeTiming.SHOW))
        assertEquals("a waiting failure stays until 확인", R.string.action_confirm,
            IncomingLinkPolicy.noticeAction(IncomingLinkNotice.SAVE_FAILED, NoticeTiming.WAIT))
        assertTrue(NoticeTiming.WAIT.persistent)
        assertFalse(NoticeTiming.SHOW.persistent)
    }

    @Test fun upperCaseSchemesAreAcceptedAndHandedOnInLowerCase() {
        val link = valid(shared("HTTPS://Example.com/A.torrent"))
        assertEquals(IncomingLinkKind.TORRENT_URL, link.kind)
        assertEquals("https://Example.com/A.torrent", link.value)
        assertEquals(magnet, valid(shared("MAGNET:" + magnet.removePrefix("magnet:"))).value)
        assertEquals(IncomingLinkError.HTTPS_REQUIRED, reason(shared("HTTP://example.com/a.torrent")))
    }

    @Test fun policyAllowsOnlySavingDuringPlaybackAndDetectsPlaylistLinks() {
        val link = valid(IncomingLinkParser.link(magnet))
        val match = PlaylistMatch(magnet, "", 0, complete = false)
        val state = IncomingLinkPolicy.state(link, match, playbackActive = true)
        assertEquals(PlaylistMembership.IN_PLAYLIST, state.membership)
        assertEquals(setOf(IncomingLinkAction.SAVE), state.allowed)
        assertEquals(PlaylistMembership.NEW, IncomingLinkPolicy.membership(link, null))
        assertEquals(IncomingLinkAction.entries.toSet(), IncomingLinkPolicy.allowed(playbackActive = false))
        val url = valid(IncomingLinkParser.link("https://example.com/a.torrent"))
        assertEquals(PlaylistMembership.UNKNOWN, IncomingLinkPolicy.membership(url, match))
    }

    @Test fun errorReasonsRegisterTheirSheetTexts() {
        assertEquals(IncomingLinkError.entries.size, IncomingLinkError.entries.map { it.message }.toSet().size)
    }

    @Test fun recreatedOrHistoryLaunchesDoNotHandleTheIntentAgain() {
        assertTrue(IncomingLinkPolicy.shouldHandle(recreated = false, launchedFromHistory = false))
        assertFalse(IncomingLinkPolicy.shouldHandle(recreated = true, launchedFromHistory = false))
        assertFalse(IncomingLinkPolicy.shouldHandle(recreated = false, launchedFromHistory = true))
    }
}
