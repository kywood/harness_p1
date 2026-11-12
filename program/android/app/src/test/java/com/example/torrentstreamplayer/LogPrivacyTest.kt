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
    private val logCall = Regex("""\bLog\.(wtf|[dviwe])\(""")
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

    @Test fun logsNeverPassTheThrowableOrItsText() {
        // Log.e(TAG, msg, t)나 "$t"·t.message는 예외 원문과 스택을 릴리스 logcat에 남긴다. EngineFailureLog.describe(t)를 쓴다.
        val leaking = File("src/main/java").walkTopDown()
            .filter { it.extension == "kt" }
            .flatMap { file ->
                val source = file.readText()
                throwableLeaks(source).map { "${file.path}:${source.substring(0, it).count { c -> c == '\n' } + 1}" }
            }
            .toList()
        assertEquals("logs that pass a Throwable or its text", emptyList<String>(), leaking)
    }

    @Test fun throwableScanCatchesEveryLeakShape() {
        val d = '$'
        val leaking = listOf(
            "Log.w(TAG, \"Seek, failed\",\n    error)",
            "Log.wtf(TAG, \"broken\", t)",
            "Log.w(TAG, t)",
            "catch (oops: Exception) { Log.w(TAG, oops) }",
            "Log.e(TAG, \"failed: ${d}t\")",
            "Log.e(TAG, \"failed: ${d}{ it }\")",
            "Log.e(TAG, \"failed: ${d}error.message\")",
            "Log.e(TAG, \"failed: ${d}{e.message}\")",
            "Log.e(TAG, \"x)\" + t.message)",
            "Log.e(TAG, \"failed \" + failure.toString())",
            "Log.e(TAG, \"failed ${d}{EngineFailureLog.describe(t)} ${d}{t.localizedMessage}\")",
        )
        leaking.forEach { assertEquals(it, 1, throwableLeaks(it).count()) }
    }

    @Test fun throwableScanAllowsDescribedFailuresAndLiteralText() {
        val d = '$'
        val passing = listOf(
            "Log.e(TAG, \"a, (b\" +\n    \"c: ${d}{EngineFailureLog.describe(t, BuildConfig.DEBUG)}\")",
            "Log.w(TAG, \"Consent: ${d}{error.errorCode}\")",
            "Log.i(TAG, \"t.message is plain text, (not code\\\")\")",
            "Log.i(TAG, \"costs \\${d}t\")",
            "Log.w(TAG, \"retry\")",
            "Log.e(\n    TAG,\n    \"failed: ${d}{EngineFailureLog.describe(t)}\",\n)",
            "Log.w(TAG, \"Kept ${d}{it.kept} entries\")",
            "Log.w(TAG, \"Unable to save: ${d}{EngineFailureLog.describe(it)}\")",
        )
        passing.forEach { assertEquals(it, 0, throwableLeaks(it).count()) }
    }

    /** 예외 객체나 그 원문을 로그에 넘기는 `Log` 호출의 시작 위치. */
    private fun throwableLeaks(source: String): Sequence<Int> {
        val names = (throwableNames + catchName.findAll(source).map { it.groupValues[1] }).joinToString("|")
        val bare = Regex("""^($names)$""")
        // `$t`·`${t}`는 toString(원문)을 남긴다. `${t.member}`는 원문 멤버일 때만 exceptionText가 잡는다.
        val interpolated = Regex("""\$($names)\b(?!\.)|\$\{\s*($names)\s*}""")
        return logCall.findAll(source).filter { call ->
            val args = SourceCallScanner.arguments(source, call.range.last + 1)
            val messages = args.drop(1).map(SourceCallScanner::withoutDescribeCalls)
            args.size > 2 || messages.any {
                bare.matches(it) || interpolated.containsMatchIn(it) || exceptionText.containsMatchIn(it)
            }
        }.map { it.range.first }
    }

    private val catchName = Regex("""\bcatch\s*\(\s*(\w+)\s*:""")
    private val throwableNames = listOf("t", "e", "ex", "it", "error", "failure", "throwable", "cause", "exception")
    private val exceptionText = Regex("""\.(message|localizedMessage)\b|\.(toString|stackTraceToString)\(\)""")
}
