package com.example.torrentstreamplayer.ui

import com.example.torrentstreamplayer.engine.InvalidMagnetException
import com.example.torrentstreamplayer.engine.TorrentInputResolver
import com.example.torrentstreamplayer.meta.TorrentFailure
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

    @Test fun otherFailuresKeepTheirOwnMessage() {
        assertNull(InputFailureText.failure(IllegalArgumentException("토렌트 파일이 너무 큽니다.")))
        assertEquals("토렌트 파일이 너무 큽니다.", InputFailureText.ownMessage(IllegalArgumentException("토렌트 파일이 너무 큽니다.")))
        assertEquals("토렌트 다운로드 실패 (HTTP 404)", InputFailureText.ownMessage(IllegalStateException("토렌트 다운로드 실패 (HTTP 404)")))
    }

    @Test fun networkFailuresUseRegisteredTextInsteadOfHostOrEnglish() {
        listOf(UnknownHostException("Unable to resolve host \"tracker.example.com\""),
            SSLHandshakeException("Chain validation failed"),
            SocketTimeoutException("failed to connect to tracker.example.com/1.2.3.4 (port 443) after 10000ms"),
            MalformedURLException("no protocol: x")).forEach { error ->
            assertEquals(error.toString(), TorrentFailure.LINK_GENERIC, InputFailureText.failure(error))
            assertNull(InputFailureText.ownMessage(error))
        }
    }

    @Test fun libraryMessagesWithoutKoreanTextAreNeverShown() {
        assertNull(InputFailureText.ownMessage(IllegalArgumentException("Failed requirement.")))
        assertNull(InputFailureText.ownMessage(NumberFormatException("For input string: \"x\"")))
        assertNull(InputFailureText.ownMessage(RuntimeException("한국어지만 앱이 던진 형식이 아님")))
        assertNull(InputFailureText.ownMessage(IllegalStateException()))
    }
}
