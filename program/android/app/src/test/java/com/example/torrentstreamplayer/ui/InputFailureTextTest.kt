package com.example.torrentstreamplayer.ui

import com.example.torrentstreamplayer.engine.InvalidMagnetException
import com.example.torrentstreamplayer.engine.TorrentInputResolver
import com.example.torrentstreamplayer.meta.TorrentFailure
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

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
    }
}
