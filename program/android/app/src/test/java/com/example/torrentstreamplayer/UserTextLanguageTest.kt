package com.example.torrentstreamplayer

import com.example.torrentstreamplayer.engine.InvalidMagnetException
import com.example.torrentstreamplayer.engine.MagnetIdentity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        assertFalse("엔진 오류 문구는 예외 원문을 받지 않는다", strings.getValue("failure_engine").contains("%"))
    }

    @Test fun engineNeverPassesExceptionTextToTheUserCallback() {
        val leaking = File("src/main/java/com/example/torrentstreamplayer/engine").walkTopDown()
            .filter { it.extension == "kt" }
            .flatMap { file ->
                val source = file.readText()
                onErrorCalls(source)
                    .filter { exceptionText.containsMatchIn(it.second) }
                    .map { (start, _) -> "${file.name}:${source.substring(0, start).count { it == '\n' } + 1}" }
            }
            .toList()
        assertEquals("onError로 예외 원문을 넘기는 호출", emptyList<String>(), leaking)
    }

    @Test fun onErrorScanFollowsCallsAcrossLines() {
        val source = "onError(\n    TorrentFailure.ENGINE.text(\n        context, t.localizedMessage)\n)\nonError(ok)" +
            "\nonError(\"x)\" + t.message)\nonError(\"t.message is text\")"
        val calls = onErrorCalls(source).toList()
        assertEquals(4, calls.size)
        assertTrue(exceptionText.containsMatchIn(calls[0].second))
        assertFalse(exceptionText.containsMatchIn(calls[1].second))
        assertTrue("문자열 리터럴 안의 `)`에서 인자를 끊지 않는다", exceptionText.containsMatchIn(calls[2].second))
        assertFalse("문자열 리터럴 글자는 코드로 보지 않는다", exceptionText.containsMatchIn(calls[3].second))
    }

    /**
     * `onError(`부터 짝이 맞는 `)`까지의 인자 코드를 (시작 위치, 인자) 쌍으로 돌려준다. 여러 줄 호출도 한 덩어리로 보고,
     * 문자열 리터럴 글자는 빼고 템플릿만 남긴다.
     */
    private fun onErrorCalls(source: String): Sequence<Pair<Int, String>> =
        onErrorCall.findAll(source).map { match ->
            match.range.first to SourceCallScanner.arguments(source, match.range.last + 1).joinToString(", ")
        }

    private val onErrorCall = Regex("""\bonError\(""")
    private val exceptionText = Regex("""\.(localizedMessage|message)\b""")

    @Test fun invalidMagnetIsReportedWithItsOwnTypeSoTheUiCanLocalizeIt() {
        assertThrows(InvalidMagnetException::class.java) { MagnetIdentity.key("magnet:?xt=urn:btih:not-a-hash") }
    }
}
