package com.example.torrentstreamplayer.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.FileNotFoundException

class EngineFailureLogTest {
    @Test fun releaseLogKeepsOnlyTheExceptionType() {
        val error = IllegalStateException("Previous download is still stopping")
        assertEquals("IllegalStateException", EngineFailureLog.describe(error, debug = false))
    }

    @Test fun debugLogKeepsTheMessageButHidesPaths() {
        val error = FileNotFoundException("/data/user/0/com.gamenuri.pieceplay/files/torrent_downloads/abc/영화.mkv (No such file)")
        val line = EngineFailureLog.describe(error, debug = true)
        assertTrue(line.startsWith("FileNotFoundException: <redacted> (No such file)"))
        assertFalse(line.contains("/data"))
        assertFalse(line.contains("영화.mkv"))
    }

    @Test fun debugLogHidesWindowsPathsUrisAndMagnets() {
        val error = IllegalStateException("메타데이터 연결 실패: C:\\tmp\\a.torrent https://example.com/x magnet:?xt=urn:btih:abc")
        val line = EngineFailureLog.describe(error, debug = true)
        assertFalse(line.contains("tmp"))
        assertFalse(line.contains("example.com"))
        assertFalse(line.contains("btih"))
        assertTrue(line.startsWith("IllegalStateException: 메타데이터 연결 실패: <redacted> <redacted> <redacted>"))
    }

    @Test fun debugLogWithoutMessageNamesTheTypeAndCallSite() {
        val line = EngineFailureLog.describe(IllegalStateException(), debug = true)
        assertTrue(line.startsWith("IllegalStateException at "))
        assertTrue(line.contains("EngineFailureLogTest"))
    }
}
