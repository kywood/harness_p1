package com.example.torrentstreamplayer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * 기본 `values/strings.xml`과 모든 번역 폴더(`values-*`)의 문자열이 맞는지 본다. 언어를 추가해도 이 테스트는 고치지 않는다.
 * - 번역할 키(문자열과 plurals) 집합이 기본과 같다. 번역하지 않는 값(`translatable="false"`)과 앱 이름은 번역 폴더에 두지 않는다.
 * - 포맷 인자(`%1$s`, `%2$d` …)의 번호와 형식이 기본과 같다. plurals는 항목마다 같은 인자를 쓴다.
 * - 인자는 어순이 다른 언어도 바꿔 쓸 수 있게 번호를 붙인다(`%s`가 아니라 `%1$s`).
 */
class StringResourceLocalesTest {
    private val res = File("src/main/res")

    private class Entry(val translatable: Boolean, val variants: List<String>)

    @Test fun everyLocaleHasTheSameKeysAsTheDefault() {
        val base = translatable(read(File(res, "values/strings.xml")))
        assertTrue("번역 폴더가 하나 이상 있다", locales().isNotEmpty())
        locales().forEach { file ->
            val entries = read(file)
            val folder = file.parentFile.name
            assertEquals("$folder: 기본에 없는 키", emptySet<String>(), entries.keys - base.keys)
            assertEquals("$folder: 번역이 빠진 키", emptySet<String>(), base.keys - entries.keys)
            val untranslatable = entries.filterValues { !it.translatable }.keys + entries.keys.filter { it == APP_NAME }
            assertEquals("$folder: 번역하지 않는 값은 기본에만 둔다", emptySet<String>(), untranslatable.toSet())
        }
    }

    @Test fun everyLocaleUsesTheSameFormatArguments() {
        val base = translatable(read(File(res, "values/strings.xml")))
        locales().forEach { file ->
            val folder = file.parentFile.name
            val mismatched = read(file).filterKeys { it in base }.mapNotNull { (key, entry) ->
                val expected = arguments(base.getValue(key).variants.last())
                entry.variants.firstOrNull { arguments(it) != expected }?.let { "$key: $it (기본 $expected)" }
            }
            assertEquals("$folder: 포맷 인자가 다른 문구", emptyList<String>(), mismatched)
        }
    }

    @Test fun pluralItemsAgreeWithEachOtherInTheDefault() {
        val mismatched = read(File(res, "values/strings.xml")).filter { (_, entry) ->
            entry.variants.map(::arguments).toSet().size > 1
        }.keys
        assertEquals("plurals 항목끼리 인자가 다름", emptySet<String>(), mismatched)
    }

    @Test fun formatArgumentsAreNumberedSoTranslationsCanReorderThem() {
        (listOf(File(res, "values/strings.xml")) + locales()).forEach { file ->
            val unnumbered = read(file).filterValues { entry -> entry.variants.any { UNNUMBERED.containsMatchIn(it) } }.keys
            assertEquals("${file.parentFile.name}: 번호 없는 인자", emptySet<String>(), unnumbered)
        }
    }

    @Test fun argumentReaderIgnoresEscapedPercent() {
        assertEquals(listOf("1\$s", "2\$d"), arguments("%2\$d%% · %1\$s"))
        assertTrue(UNNUMBERED.containsMatchIn("%s left"))
        assertTrue(!UNNUMBERED.containsMatchIn("100%% · %1\$d"))
    }

    private fun locales(): List<File> =
        res.listFiles().orEmpty().filter { it.isDirectory && it.name.startsWith("values-") }
            .map { File(it, "strings.xml") }.filter { it.isFile }.sortedBy { it.parentFile.name }

    private fun translatable(entries: Map<String, Entry>) = entries.filter { (key, entry) -> entry.translatable && key != APP_NAME }

    /** 키 → 항목. `<string>`은 값 하나, `<plurals>`는 항목 값들을 쓴다. */
    private fun read(file: File): Map<String, Entry> {
        val xml = file.readText().replace(COMMENT, "")
        val strings = STRING.findAll(xml).associate { m ->
            m.groupValues[1] to Entry(!m.groupValues[2].contains("translatable=\"false\""), listOf(m.groupValues[3]))
        }
        val plurals = PLURALS.findAll(xml).associate { m ->
            m.groupValues[1] to Entry(true, ITEM.findAll(m.groupValues[2]).map { it.groupValues[1] }.toList())
        }
        return strings + plurals
    }

    /** 문구의 포맷 인자를 번호 순서로 정렬한 목록. `%%`는 인자가 아니다. */
    private fun arguments(value: String): List<String> =
        ARGUMENT.findAll(value.replace("%%", "")).map { it.groupValues[1] }.sorted().toList()

    private companion object {
        const val APP_NAME = "app_name"
        val COMMENT = Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL)
        val STRING = Regex("<string name=\"([^\"]+)\"([^>]*)>(.*?)</string>", RegexOption.DOT_MATCHES_ALL)
        val PLURALS = Regex("<plurals name=\"([^\"]+)\"[^>]*>(.*?)</plurals>", RegexOption.DOT_MATCHES_ALL)
        val ITEM = Regex("<item quantity=\"[a-z]+\">(.*?)</item>", RegexOption.DOT_MATCHES_ALL)
        val ARGUMENT = Regex("%(\\d+\\$[a-zA-Z])")
        val UNNUMBERED = Regex("%(?!%|\\d+\\$)[-#+ 0,(]*\\d*(?:\\.\\d+)?[a-zA-Z]")
    }
}
