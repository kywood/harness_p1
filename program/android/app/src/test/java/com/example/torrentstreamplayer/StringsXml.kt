package com.example.torrentstreamplayer

import java.io.File

/**
 * 테스트용 `res/values(-언어)/strings.xml` 읽기. 문자열 테스트는 모두 이 파서 하나를 쓴다.
 * `<string>`(속성 포함)과 `<plurals>`를 읽고, 주석 안의 항목은 무시한다. 값은 XML 그대로(이스케이프를 풀지 않는다)다.
 */
internal object StringsXml {
    const val DEFAULT = "values"
    const val APP_NAME = "app_name"

    /**
     * 문자열 하나. [quantities]는 `<string>`이면 `""` 하나, `<plurals>`면 quantity("one", "other" …)별 값이다.
     * [translatable]은 `translatable="false"`가 아니면 true다.
     */
    class Entry(val translatable: Boolean, val quantities: Map<String, String>) {
        val isPlural: Boolean get() = "" !in quantities

        /** 문자열 값, plurals면 `other` 항목. */
        val value: String get() = quantities[""] ?: quantities.getValue("other")

        val variants: Collection<String> get() = quantities.values
    }

    private val res = File("src/main/res")
    private val COMMENT = Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL)
    private val STRING = Regex("<string name=\"([^\"]+)\"([^>]*)>(.*?)</string>", RegexOption.DOT_MATCHES_ALL)
    private val PLURALS = Regex("<plurals name=\"([^\"]+)\"[^>]*>(.*?)</plurals>", RegexOption.DOT_MATCHES_ALL)
    private val ITEM = Regex("<item quantity=\"([a-z]+)\">(.*?)</item>", RegexOption.DOT_MATCHES_ALL)

    /** [folder](`values`, `values-ko` …)의 키 → 항목. */
    fun read(folder: String): Map<String, Entry> {
        val xml = File(res, "$folder/strings.xml").readText().replace(COMMENT, "")
        val strings = STRING.findAll(xml).associate { m ->
            val translatable = !m.groupValues[2].contains("translatable=\"false\"")
            m.groupValues[1] to Entry(translatable, mapOf("" to m.groupValues[3]))
        }
        val plurals = PLURALS.findAll(xml).associate { m ->
            val items = ITEM.findAll(m.groupValues[2]).associate { it.groupValues[1] to it.groupValues[2] }
            m.groupValues[1] to Entry(true, items)
        }
        return strings + plurals
    }

    /** 번역할 항목만: 번역하지 않는 값과 앱 이름을 뺀다. */
    fun translatable(folder: String): Map<String, Entry> =
        read(folder).filter { (key, entry) -> entry.translatable && key != APP_NAME }

    /** 번역할 항목의 값(plurals는 `other`). */
    fun texts(folder: String): Map<String, String> = translatable(folder).mapValues { it.value.value }

    /** `strings.xml`이 있는 번역 폴더 이름(`values-ko` …). 기본 폴더는 빠진다. */
    fun locales(): List<String> =
        res.listFiles().orEmpty().filter { it.isDirectory && it.name.startsWith("$DEFAULT-") }
            .filter { File(it, "strings.xml").isFile }.map { it.name }.sorted()
}
