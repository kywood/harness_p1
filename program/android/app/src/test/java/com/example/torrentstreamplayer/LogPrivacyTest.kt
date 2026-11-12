package com.example.torrentstreamplayer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Release logcat must not reveal what the user watches (file names, paths, links, stream requests). */
class LogPrivacyTest {
    private val sources: List<Pair<String, String>> by lazy {
        File("src/main/java").walkTopDown().filter { it.extension == "kt" }.flatMap { file ->
            file.readLines().mapIndexed { index, line -> "${file.name}:${index + 1}" to line }
        }.toList()
    }
    private val logCall = Regex("""\bLog\.([dviwe])\(""")
    private val sensitive = Regex("""\$\{?(selectedRelativePath|candidate|videoFile|selectedVideoFile|uri|url|magnet|magnetUrl|path|fileName|name)\b""")

    @Test fun debugAndVerboseLogsOnlyRunInDebugBuilds() {
        assertTrue("src/main/java not found", sources.isNotEmpty())
        val ungated = sources.filter { (_, line) ->
            logCall.findAll(line).any { it.groupValues[1] in setOf("d", "v") } && !line.contains("BuildConfig.DEBUG")
        }.map { it.first }
        assertEquals("Log.d/Log.v outside BuildConfig.DEBUG", emptyList<String>(), ungated)
    }

    @Test fun noLogInterpolatesFileNamesPathsOrLinks() {
        assertTrue("src/main/java not found", sources.isNotEmpty())
        val leaking = sources.filter { (_, line) -> logCall.containsMatchIn(line) && sensitive.containsMatchIn(line) }
            .map { it.first }
        assertEquals("logs that interpolate user content", emptyList<String>(), leaking)
    }

    @Test fun engineLogsNeverPassTheThrowable() {
        // Log.e(TAG, msg, t)는 예외 메시지 원문과 스택을 릴리스 logcat에 남긴다. EngineFailureLog.describe(t)를 쓴다.
        val leaking = File("src/main/java/com/example/torrentstreamplayer/engine").walkTopDown()
            .filter { it.extension == "kt" }
            .flatMap { file ->
                val source = file.readText()
                logCall.findAll(source).filter { topLevelArguments(source, it.range.last + 1) > 2 }
                    .map { "${file.name}:${source.substring(0, it.range.first).count { c -> c == '\n' } + 1}" }
            }
            .toList()
        assertEquals("engine logs that pass a Throwable", emptyList<String>(), leaking)
    }

    @Test fun argumentCountSkipsStringsAndNestedCallsAcrossLines() {
        val passing = "Log.e(TAG, \"a, (b\" +\n    \"c: ${'$'}{EngineFailureLog.describe(t, BuildConfig.DEBUG)}\")"
        val leaking = "Log.w(TAG, \"Seek, failed\",\n    error)"
        assertEquals(2, topLevelArguments(passing, passing.indexOf('(') + 1))
        assertEquals(3, topLevelArguments(leaking, leaking.indexOf('(') + 1))
    }

    /** [start]는 여는 괄호 바로 뒤. 짝이 맞는 `)`까지 최상위 인자 수를 센다. 문자열 리터럴 안은 건너뛴다. */
    private fun topLevelArguments(source: String, start: Int): Int {
        var depth = 1
        var commas = 0
        var i = start
        while (i < source.length && depth > 0) {
            when (source[i]) {
                '"' -> {
                    i++
                    while (i < source.length && source[i] != '"') i += if (source[i] == '\\') 2 else 1
                }
                '(' -> depth++
                ')' -> depth--
                ',' -> if (depth == 1) commas++
            }
            i++
        }
        return commas + 1
    }
}
