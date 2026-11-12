package com.example.torrentstreamplayer.engine

import com.example.torrentstreamplayer.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.FileNotFoundException
import java.io.IOException

class EngineFailureLogTest {
    private val hash40 = "c12fe1c06bba254a9dc9f519b335aa7c1367a88a"
    private val hash64 = "4b7c6d3e2f1a0b9c8d7e6f5a4b3c2d1e0f9a8b7c6d5e4f3a2b1c0d9e8f7a6b5c"

    // 리뷰(#95)에서 가림 정규식을 빠져나간 사례들. 메시지 원문을 아예 넣지 않으므로 어떤 형식이든 남지 않아야 한다.
    private val leakingMessages = listOf(
        "/storage/emulated/0/Download/My Secret Film 2024.mkv: open failed: ENOENT",
        "content://media/external/video/My Secret Film.mkv",
        "magnet:?xt=urn:btih:$hash40&dn=My Secret Film",
        "file:Secret.Film.2024.mkv",
        "unable to open Secret.Film.2024.mkv",
        "info hash $hash40 / $hash64",
        "C:\\tmp\\Secret Film.torrent https://example.com/Secret",
    )
    private val forbidden = listOf("Secret", "Film", "storage", "content:", "file:", "magnet", "btih", "example.com",
        hash40, hash64, "ENOENT")

    @Test fun releaseLogKeepsOnlyTheExceptionType() {
        val error = IllegalStateException("Previous download is still stopping")
        assertEquals("IllegalStateException", EngineFailureLog.describe(error, debug = false))
    }

    @Test fun releaseLogNamesTheCauseChain() {
        val error = IllegalStateException("outer", IOException("inner", FileNotFoundException("deepest")))
        assertEquals(
            "IllegalStateException <- caused by IOException <- caused by FileNotFoundException",
            EngineFailureLog.describe(error, debug = false),
        )
    }

    @Test fun noLogLeaksTitlesPathsOrHashesFromMessages() {
        for (message in leakingMessages) {
            val error = IllegalStateException(message, FileNotFoundException(message))
            for (debug in listOf(false, true)) {
                val line = EngineFailureLog.describe(error, debug)
                for (word in forbidden) {
                    assertFalse("debug=$debug, \"$message\" → $line", line.contains(word, ignoreCase = true))
                }
            }
        }
    }

    @Test fun debugLogNamesTheTypeChainAndCallSite() {
        val error = IllegalStateException("x", FileNotFoundException("y"))
        val line = EngineFailureLog.describe(error, debug = true)
        assertTrue(line.startsWith("IllegalStateException <- caused by FileNotFoundException at "))
        assertTrue(line.contains("EngineFailureLogTest"))
    }

    @Test fun debugLogWithoutMessageNamesTheTypeAndCallSite() {
        val line = EngineFailureLog.describe(IllegalStateException(), debug = true)
        assertTrue(line.startsWith("IllegalStateException at "))
        assertTrue(line.contains("EngineFailureLogTest"))
    }

    @Test fun defaultDescribeFollowsTheBuildType() {
        val error = IllegalStateException("Secret Film", IOException("Secret Film"))
        assertEquals(EngineFailureLog.describe(error, BuildConfig.DEBUG), EngineFailureLog.describe(error))
        assertFalse(EngineFailureLog.describe(error).contains("Secret"))
    }
}
