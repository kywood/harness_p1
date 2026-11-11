package com.example.torrentstreamplayer

import org.junit.Assert.assertEquals
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
        val ungated = sources.filter { (_, line) ->
            logCall.findAll(line).any { it.groupValues[1] in setOf("d", "v") } && !line.contains("BuildConfig.DEBUG")
        }.map { it.first }
        assertEquals("Log.d/Log.v outside BuildConfig.DEBUG", emptyList<String>(), ungated)
    }

    @Test fun noLogInterpolatesFileNamesPathsOrLinks() {
        val leaking = sources.filter { (_, line) -> logCall.containsMatchIn(line) && sensitive.containsMatchIn(line) }
            .map { it.first }
        assertEquals("logs that interpolate user content", emptyList<String>(), leaking)
    }
}
