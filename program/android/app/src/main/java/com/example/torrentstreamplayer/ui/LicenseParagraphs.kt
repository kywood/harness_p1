package com.example.torrentstreamplayer.ui

/**
 * 라이선스 전문을 문단으로 나눈다. LGPL 전문(약 500줄)을 `Text` 하나로 그리면 느려서 문단마다 목록 항목으로 그린다.
 * 빈 줄이 문단 경계이고, 문단 안의 줄바꿈과 들여쓰기는 그대로 둔다.
 */
internal object LicenseParagraphs {
    private val BLANK_LINES = Regex("""\n(?:[ \t]*\n)+""")

    fun split(text: String): List<String> =
        text.replace("\r\n", "\n").split(BLANK_LINES).map { it.trimEnd() }.filter { it.isNotBlank() }
}
