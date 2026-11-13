package com.example.torrentstreamplayer.ui

import com.example.torrentstreamplayer.engine.InvalidMagnetException
import com.example.torrentstreamplayer.engine.TorrentInputResolver
import com.example.torrentstreamplayer.meta.TorrentFailure
import com.example.torrentstreamplayer.meta.UserFacingFailure
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.MalformedURLException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

class InputFailureTextTest {
    @Test fun invalidMagnetUsesRegisteredText() {
        assertEquals(TorrentFailure.INVALID_MAGNET, InputFailureText.failure(InvalidMagnetException()))
    }

    @Test fun unreadableTorrentFileUsesRegisteredText() {
        assertEquals(TorrentFailure.METADATA_DECODE,
            InputFailureText.failure(TorrentInputResolver.InvalidTorrentFileException(RuntimeException("x"))))
    }

    @Test fun appFailuresCarryTheirRegisteredTextNotAMessage() {
        val invalid = TorrentFailure.TORRENT_TOO_LARGE.invalid()
        assertTrue(invalid is IllegalArgumentException)
        assertEquals(TorrentFailure.TORRENT_TOO_LARGE, InputFailureText.failure(invalid))
        val blocked = TorrentFailure.TORRENT_HTTP.blocked(404)
        assertTrue(blocked is IllegalStateException)
        assertEquals(TorrentFailure.TORRENT_HTTP, InputFailureText.failure(blocked))
        assertEquals(listOf<Any>(404), (blocked as UserFacingFailure).args)
        assertEquals("메시지에는 항목 이름만 담는다", "TORRENT_HTTP", blocked.message)
    }

    @Test fun plainExceptionsWithKoreanTextAreNotRegistered() {
        assertNull(InputFailureText.failure(IllegalArgumentException("토렌트 파일이 너무 큽니다.")))
        assertNull(InputFailureText.failure(IllegalStateException("Failed requirement.")))
    }

    @Test fun networkFailuresUseRegisteredTextInsteadOfHostOrEnglish() {
        listOf(UnknownHostException("Unable to resolve host \"tracker.example.com\""),
            SSLHandshakeException("Chain validation failed"),
            SocketTimeoutException("failed to connect to tracker.example.com/1.2.3.4 (port 443) after 10000ms"),
            MalformedURLException("no protocol: x")).forEach { error ->
            assertEquals(error.toString(), TorrentFailure.LINK_GENERIC, InputFailureText.failure(error))
            assertFalse(error is UserFacingFailure)
        }
    }

    @Test fun libraryExceptionsAreNeverAppFailures() {
        listOf(IllegalArgumentException("Failed requirement."), NumberFormatException("For input string: \"x\""),
            RuntimeException("한국어지만 앱이 던진 형식이 아님"), IllegalStateException()).forEach {
            assertFalse(it is UserFacingFailure)
            assertNull(InputFailureText.failure(it))
        }
    }
}
