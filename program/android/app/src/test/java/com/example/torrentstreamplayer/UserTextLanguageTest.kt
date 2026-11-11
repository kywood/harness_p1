package com.example.torrentstreamplayer

import com.example.torrentstreamplayer.engine.InvalidMagnetException
import com.example.torrentstreamplayer.engine.MagnetIdentity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class UserTextLanguageTest {
    private val strings: Map<String, String> by lazy {
        val xml = File("src/main/res/values/strings.xml").readText()
        Regex("<string name=\"([^\"]+)\">([^<]*)</string>").findAll(xml)
            .associate { it.groupValues[1] to it.groupValues[2] }
    }
    private val hangul = Regex("[가-힣]")

    @Test fun everyUserStringExceptTheBrandNameIsKorean() {
        val english = strings.filterKeys { it != "app_name" }.filterValues { !hangul.containsMatchIn(it) }
        assertEquals("strings without Korean text", emptyMap<String, String>(), english)
    }

    @Test fun everyRegisteredTorrentFailureHasAString() {
        val failureKeys = strings.keys.filter { it.startsWith("failure_") }.toSet()
        val expected = setOf("failure_invalid_magnet", "failure_metadata_timeout", "failure_metadata_decode",
            "failure_no_video", "failure_download_setup", "failure_engine", "failure_unknown")
        assertEquals(expected, failureKeys)
        assertTrue(strings.getValue("failure_engine").contains("%1\$s"))
    }

    @Test fun invalidMagnetIsReportedWithItsOwnTypeSoTheUiCanLocalizeIt() {
        assertThrows(InvalidMagnetException::class.java) { MagnetIdentity.key("magnet:?xt=urn:btih:not-a-hash") }
    }
}
