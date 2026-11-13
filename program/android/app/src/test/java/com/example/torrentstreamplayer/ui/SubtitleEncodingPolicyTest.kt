package com.example.torrentstreamplayer.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.charset.Charset

class SubtitleEncodingPolicyTest {
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
