package com.example.torrentstreamplayer

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/**
 * 사용자 문구는 `strings.xml`(언어별 `values-*`)에만 둔다. Kotlin 소스에 한글 문자열 리터럴이 새로 생기면 실패한다.
 * 주석과 로그(`Log.` 호출이 있는 줄)는 보지 않는다. 화면에 보이지 않는 데이터 값만 [ALLOWED]에 이유와 함께 둔다.
 */
class SourceTextLiteralTest {
    @Test fun kotlinSourcesHaveNoHangulStringLiterals() {
        val found = File("src/main/java").walkTopDown().filter { it.extension == "kt" }.flatMap { file ->
            literals(file.readText()).filter { (line, text) ->
                HANGUL.containsMatchIn(text) && !line.contains("Log.") && text !in ALLOWED[file.name].orEmpty()
            }.map { (_, text) -> "${file.name}: \"$text\"" }
        }.toList()
        assertEquals("strings.xml로 옮길 한글 리터럴", emptyList<String>(), found)
    }

    @Test fun scannerSkipsCommentsAndKeepsTheLineOfEachLiteral() {
        val source = "// \"주석\"\n/* \"블록 \"주석\" */ val a = \"보임\"\nLog.w(TAG, \"로그\")\nval b = \"\"\"원시 \"x\" 문자열\"\"\""
        assertEquals(listOf("보임", "로그", "원시 \"x\" 문자열"), literals(source).map { it.second })
        assertEquals("Log.w(TAG, \"로그\")", literals(source)[1].first)
    }

    /** (리터럴이 시작한 줄, 리터럴 글자) 목록. 주석 안의 따옴표는 리터럴로 보지 않는다. */
    private fun literals(source: String): List<Pair<String, String>> {
        val out = mutableListOf<Pair<String, String>>()
        var i = 0
        fun lineAt(index: Int): String {
            val start = source.lastIndexOf('\n', index - 1) + 1
            val end = source.indexOf('\n', index).let { if (it < 0) source.length else it }
            return source.substring(start, end).trim()
        }
        while (i < source.length) {
            when {
                source.startsWith("//", i) -> i = source.indexOf('\n', i).let { if (it < 0) source.length else it }
                source.startsWith("/*", i) -> i = source.indexOf("*/", i + 2).let { if (it < 0) source.length else it + 2 }
                source.startsWith("\"\"\"", i) -> {
                    val end = source.indexOf("\"\"\"", i + 3).let { if (it < 0) source.length else it }
                    out += lineAt(i) to source.substring(i + 3, end)
                    i = end + 3
                }
                source[i] == '"' -> {
                    var j = i + 1
                    while (j < source.length && source[j] != '"' && source[j] != '\n') j += if (source[j] == '\\') 2 else 1
                    out += lineAt(i) to source.substring(i + 1, minOf(j, source.length))
                    i = j + 1
                }
                source[i] == '\'' -> {
                    var j = i + 1
                    while (j < source.length && source[j] != '\'' && source[j] != '\n') j += if (source[j] == '\\') 2 else 1
                    i = j + 1
                }
                else -> i++
            }
        }
        return out
    }

    private companion object {
        val HANGUL = Regex("[가-힣]")

        /** 파일 이름 → 허용한 리터럴. 화면 문구가 아닌 값만 둔다. */
        val ALLOWED = mapOf(
            // 자막 파일 이름에서 언어를 알아보는 표지(예: "영화.한글.srt"). 화면에 보이지 않는다.
            "SubtitleLanguage.kt" to setOf("한글", "한국어"),
            // 이미 저장된 재생 목록 제목과 맞춰야 하는 자리 표시 앞부분. 화면은 playlist_untitled로 바꿔 보인다.
            "MagnetHistory.kt" to setOf("토렌트 "),
        )
    }
}
