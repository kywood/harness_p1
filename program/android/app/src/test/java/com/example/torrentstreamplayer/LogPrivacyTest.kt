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
                throwableLeaks(source, file.name)
                    .map { "${file.path}:${source.substring(0, it).count { c -> c == '\n' } + 1}" }
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
            "x.onFailure {\n    Log.e(TAG, \"failed: ${d}{ it }\")\n}",
            "Log.e(TAG, \"failed: ${d}{ e }\")",
            "Log.e(TAG, \"failed: ${d}error.message\")",
            "Log.e(TAG, \"failed: ${d}{e.message}\")",
            "Log.e(TAG, \"x)\" + t.message)",
            "Log.e(TAG, \"failed \" + failure.toString())",
            "Log.e(TAG, \"failed ${d}{EngineFailureLog.describe(t)} ${d}{t.localizedMessage}\")",
            "Log.e(TAG, Log.getStackTraceString(t))",
            "Log.e(TAG, \"failed: ${d}{t.cause}\")",
            "runCatching { work() }.onFailure { Log.w(TAG, \"failed: ${d}it\") }",
            "runCatching { work() }.onFailure { err -> Log.w(TAG, err) }",
            "Log.w(TAG, \"Consent: ${d}{error.message}\")",
        )
        leaking.forEach { assertEquals(it, 1, throwableLeaks(it, "Other.kt").count()) }
    }

    @Test fun allowedExpressionsOnlyPassInTheirOwnFile() {
        val consent = "Log.w(TAG, \"Consent: ${'$'}{error.errorCode} ${'$'}{error.message}\")"
        assertEquals(0, throwableLeaks(consent, "AdConsent.kt").count())
        assertEquals(1, throwableLeaks(consent, "Other.kt").count())
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
            "Log.w(TAG, \"Kept ${d}it entries\")",
            "Log.w(TAG, \"Status: ${d}{state.message}\")",
            "runCatching { work() }.onFailure { Log.w(TAG, \"failed: ${d}{EngineFailureLog.describe(it)}\") }",
        )
        passing.forEach { assertEquals(it, 0, throwableLeaks(it, "Other.kt").count()) }
    }

    /**
     * 예외 객체나 그 원문을 로그에 넘기는 `Log` 호출의 시작 위치.
     *
     * 예외로 보는 이름은 [throwableNames], 그 파일의 `catch (x: …)` 변수, `onFailure { x -> … }`의 파라미터다.
     * `it`은 `onFailure { … }` 블록 안의 호출에서만 예외로 본다(`"Kept $it entries"` 같은 정상 로그를 잡지 않으려고).
     *
     * 한계: 그 밖의 람다 파라미터(`onError = { err -> Log.w(TAG, err) }`, 다른 함수의 `it`)는 이름이 목록에 없으면
     * 2인자·템플릿 형태를 놓친다(인자 3개짜리는 이름과 상관없이 잡는다). 메시지를 미리 변수에 담는 경우
     * (`val m = "…$t"; Log.w(TAG, m)`)도 잡지 못한다.
     */
    private fun throwableLeaks(source: String, fileName: String): Sequence<Int> {
        val failureBlocks = onFailureBlock.findAll(source)
            .map { it.range.last..SourceCallScanner.closingIndex(source, it.range.last + 1) }.toList()
        val fileNames = throwableNames + catchName.findAll(source).map { it.groupValues[1] } +
            onFailureParameter.findAll(source).map { it.groupValues[1] }
        val allowed = allowedExpressions[fileName].orEmpty()
        return logCall.findAll(source).filter { call ->
            val inFailureBlock = failureBlocks.any { call.range.first in it }
            val names = (if (inFailureBlock) fileNames + "it" else fileNames).joinToString("|")
            val bare = Regex("""^($names)$""")
            // `$t`·`${t}`는 toString(원문)을 남긴다. `${t.member}`는 원문 멤버일 때만 아래에서 잡는다.
            val interpolated = Regex("""\$($names)\b(?!\.)|\$\{\s*($names)\s*}""")
            val namedText = Regex("""\b($names)\.(message|localizedMessage)\b""")
            val args = SourceCallScanner.arguments(source, call.range.last + 1)
            val messages = args.drop(1).map { arg ->
                allowed.fold(SourceCallScanner.withoutDescribeCalls(arg)) { code, expression ->
                    code.replace(expression, "")
                }
            }
            args.size > 2 || messages.any {
                bare.matches(it) || interpolated.containsMatchIn(it) || namedText.containsMatchIn(it) ||
                    exceptionText.containsMatchIn(it)
            }
        }.map { it.range.first }
    }

    private val catchName = Regex("""\bcatch\s*\(\s*(\w+)\s*:""")
    private val onFailureBlock = Regex("""\bonFailure\s*\{""")
    private val onFailureParameter = Regex("""\bonFailure\s*\{\s*(\w+)\s*->""")
    private val throwableNames = listOf("t", "e", "ex", "error", "failure", "throwable", "cause", "exception")
    /** 이름과 상관없이 예외 원문을 문자열로 만드는 식. */
    private val exceptionText = Regex("""\.cause\b|\.(toString|stackTraceToString)\(\)|\bLog\.getStackTraceString\(""")

    /**
     * 파일별로 허용하는 식. 예외가 아닌 객체가 예외 이름을 쓰는 정상 사례만 둔다.
     * - `AdConsent.kt`의 `error.message`: UMP `FormError`(Throwable 아님)의 SDK 진단 문구. 동의 실패 원인을 가리는 유일한 정보다.
     */
    private val allowedExpressions = mapOf(
        "AdConsent.kt" to listOf("error.message"),
    )
}
