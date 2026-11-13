package com.example.torrentstreamplayer.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.Charset

class SubtitleEncodingPolicyTest {
    private companion object {
        const val REPLACEMENT = "�"
    }

    private val korean = "1\n00:00:01,000 --> 00:00:03,000\n안녕하세요, 한글 자막입니다.\n"

    @Test fun utf8IsLeftAsItIs() {
        assertEquals(SubtitleEncoding.UTF8, SubtitleEncodingPolicy.detect(korean.toByteArray(Charsets.UTF_8)))
        assertEquals(SubtitleEncoding.UTF8, SubtitleEncodingPolicy.detect("plain ASCII".toByteArray()))
    }

    @Test fun utf8WithBomIsLeftAsItIs() {
        val bytes = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + korean.toByteArray(Charsets.UTF_8)
        assertEquals(SubtitleEncoding.UTF8, SubtitleEncodingPolicy.detect(bytes))
    }

    @Test fun koreanCp949IsReencodedAndDecodesBack() {
        val bytes = korean.toByteArray(Charset.forName("x-windows-949"))
        assertEquals(SubtitleEncoding.CP949, SubtitleEncodingPolicy.detect(bytes))
        assertEquals(korean, SubtitleEncodingPolicy.decodeCp949(bytes))
    }

    @Test fun aCutOffLastByteKeepsTheFileKorean() {
        val bytes = korean.toByteArray(Charset.forName("x-windows-949")) + 0xB0.toByte()
        assertEquals(SubtitleEncoding.CP949, SubtitleEncodingPolicy.detect(bytes))
        val text = SubtitleEncodingPolicy.decodeCp949(bytes)!!
        assertEquals(korean + REPLACEMENT, text)
    }

    @Test fun aBrokenPairInTheMiddleBecomesAReplacementCharacter() {
        // 0xC9 0x41 looks like a CP949 pair but maps to nothing (leads from 0xC7 only take trails from 0xA1).
        val cp949 = Charset.forName("x-windows-949")
        val bytes = korean.toByteArray(cp949) + byteArrayOf(0xC9.toByte(), 0x41) + korean.toByteArray(cp949)
        assertEquals(SubtitleEncoding.CP949, SubtitleEncodingPolicy.detect(bytes))
        val text = SubtitleEncodingPolicy.decodeCp949(bytes)!!
        assertTrue(text.startsWith(korean) && text.endsWith(korean))
        assertTrue(text.contains(REPLACEMENT))
    }

    @Test fun cp1252EnglishWithCurlyQuotesIsNotKorean() {
        val english = "1\n00:00:01,000 --> 00:00:03,000\n“It’s fine,” she said. Café.\n"
        val bytes = english.toByteArray(Charset.forName("windows-1252"))
        assertEquals(SubtitleEncoding.OTHER, SubtitleEncodingPolicy.detect(bytes))
    }

    @Test fun utf16BomIsLeftAsItIs() {
        assertEquals(SubtitleEncoding.UTF16, SubtitleEncodingPolicy.detect(korean.toByteArray(Charsets.UTF_16LE)
            .let { byteArrayOf(0xFF.toByte(), 0xFE.toByte()) + it }))
    }
}
