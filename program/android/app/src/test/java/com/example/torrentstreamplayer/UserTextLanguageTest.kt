package com.example.torrentstreamplayer

import com.example.torrentstreamplayer.engine.InvalidMagnetException
import com.example.torrentstreamplayer.engine.MagnetIdentity
import com.example.torrentstreamplayer.meta.TorrentFailure
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class UserTextLanguageTest {
    /** 기본(영어) 문구. 번역하지 않는 값(속성이 붙은 항목)은 빠진다. */
    private val strings: Map<String, String> by lazy { read("values") }
    private val korean: Map<String, String> by lazy { read("values-ko") }
    private val hangul = Regex("[가-힣]")

    private fun read(folder: String): Map<String, String> {
        val xml = File("src/main/res/$folder/strings.xml").readText()
        return Regex("<string name=\"([^\"]+)\">([^<]*)</string>").findAll(xml)
            .associate { it.groupValues[1] to it.groupValues[2] }
    }

    @Test fun everyKoreanStringIsKorean() {
        val notKorean = korean.filterValues { !hangul.containsMatchIn(it) }
        assertEquals("values-ko strings without Korean text", emptyMap<String, String>(), notKorean)
    }

    @Test fun defaultStringsAreEnglishWithoutHangul() {
        val withHangul = strings.filterValues { hangul.containsMatchIn(it) }
        assertEquals("values strings with Korean text", emptyMap<String, String>(), withHangul)
    }

    @Test fun everyRegisteredTorrentFailureHasAString() {
        val failureKeys = strings.keys.filter { it.startsWith("failure_") }.toSet() - "failure_unknown"
        val registered = TorrentFailure.entries.map { failure ->
            R.string::class.java.fields.single { it.getInt(null) == failure.message }.name
        }.toSet()
        assertEquals("failure_ 문구는 TorrentFailure에 등록해 쓴다", failureKeys, registered - "error_link_generic")
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
