package com.example.torrentstreamplayer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 기본 `values/strings.xml`과 모든 번역 폴더(`values-*`)의 문자열이 맞는지 본다. 언어를 추가해도 이 테스트는 고치지 않는다.
 * - 번역할 키(문자열과 plurals) 집합이 기본과 같다. 번역하지 않는 값(`translatable="false"`)과 앱 이름은 번역 폴더에 두지 않는다.
 * - 포맷 인자(`%1$s`, `%2$d` …)의 번호와 형식이 기본과 같다. plurals는 항목마다 같은 인자를 쓴다.
 * - 인자는 어순이 다른 언어도 바꿔 쓸 수 있게 번호를 붙인다(`%s`가 아니라 `%1$s`).
 * plurals의 quantity가 언어마다 다 있는지(`many` 등)는 lint `MissingQuantity`가 본다.
 */
class StringResourceLocalesTest {
    @Test fun everyLocaleHasTheSameKeysAsTheDefault() {
        val base = StringsXml.translatable(StringsXml.DEFAULT)
        assertTrue("번역 폴더가 하나 이상 있다", StringsXml.locales().isNotEmpty())
        StringsXml.locales().forEach { folder ->
            val entries = StringsXml.read(folder)
            assertEquals("$folder: 기본에 없는 키", emptySet<String>(), entries.keys - base.keys)
            assertEquals("$folder: 번역이 빠진 키", emptySet<String>(), base.keys - entries.keys)
            val untranslatable =
                entries.filter { (key, entry) -> !entry.translatable || key == StringsXml.APP_NAME }.keys
            assertEquals("$folder: 번역하지 않는 값은 기본에만 둔다", emptySet<String>(), untranslatable)
        }
    }

    @Test fun everyLocaleUsesTheSameFormatArguments() {
        val base = StringsXml.translatable(StringsXml.DEFAULT)
        StringsXml.locales().forEach { folder ->
            val mismatched = StringsXml.read(folder).filterKeys { it in base }.mapNotNull { (key, entry) ->
                val expected = arguments(base.getValue(key).value)
                entry.variants.firstOrNull { arguments(it) != expected }?.let { "$key: $it (기본 $expected)" }
            }
            assertEquals("$folder: 포맷 인자가 다른 문구", emptyList<String>(), mismatched)
        }
    }

    @Test fun pluralItemsAgreeWithEachOtherInTheDefault() {
        val mismatched = StringsXml.read(StringsXml.DEFAULT).filter { (_, entry) ->
            entry.variants.map(::arguments).toSet().size > 1
        }.keys
        assertEquals("plurals 항목끼리 인자가 다름", emptySet<String>(), mismatched)
    }

    @Test fun formatArgumentsAreNumberedSoTranslationsCanReorderThem() {
        (listOf(StringsXml.DEFAULT) + StringsXml.locales()).forEach { folder ->
            val unnumbered = StringsXml.read(folder).filterValues { entry ->
                entry.variants.any { UNNUMBERED.containsMatchIn(it) }
            }.keys
            assertEquals("$folder: 번호 없는 인자", emptySet<String>(), unnumbered)
        }
    }

    @Test fun argumentReaderIgnoresEscapedPercent() {
        assertEquals(listOf("1\$s", "2\$d"), arguments("%2\$d%% · %1\$s"))
        assertTrue(UNNUMBERED.containsMatchIn("%s left"))
        assertFalse(UNNUMBERED.containsMatchIn("100%% · %1\$d"))
    }

    @Test fun pluralValueIsTheOtherItemWhateverTheOrder() {
        val entry = StringsXml.Entry(true, linkedMapOf("other" to "%1\$d files", "one" to "%1\$d file"))
        assertTrue(entry.isPlural)
        assertEquals("%1\$d files", entry.value)
    }

    /** 문구의 포맷 인자를 번호 순서로 정렬한 목록. `%%`는 인자가 아니다. */
    private fun arguments(value: String): List<String> =
        ARGUMENT.findAll(value.replace("%%", "")).map { it.groupValues[1] }.sorted().toList()

    private companion object {
        val ARGUMENT = Regex("%(\\d+\\$[a-zA-Z])")
        val UNNUMBERED = Regex("%(?!%|\\d+\\$)[-#+ 0,(]*\\d*(?:\\.\\d+)?[a-zA-Z]")
    }
}
