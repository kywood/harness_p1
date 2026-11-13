package com.example.torrentstreamplayer.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.charset.Charset
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class SubtitleCopiesTest {
    @get:Rule val temp = TemporaryFolder()
    private val text = "1\n00:00:01,000 --> 00:00:03,000\n한글 자막입니다.\n"
    private val cp949 = Charset.forName("x-windows-949")

    private fun file(name: String, bytes: ByteArray) = File(temp.newFolder(), name).apply { writeBytes(bytes) }

    @Test fun olderLeftoversGoBeforeTheFirstCopyAndOnlyCp949IsCopied() {
        val parent = temp.newFolder("copies")
        val crashed = File(parent, "100-old").apply { mkdirs() }.also { File(it, "0_old.smi").writeText("old") }
        val copies = SubtitleCopies(parent, startedAtMs = 200)
        val korean = file("Movie.smi", text.toByteArray(cp949))
        val utf8 = file("Movie.srt", text.toByteArray(Charsets.UTF_8))
        copies.prepare(korean)
        copies.prepare(utf8)
        assertFalse(crashed.exists())
        val copy = copies.copyOf(korean)
        assertNotEquals(korean, copy)
        assertEquals(parent.canonicalFile, copy.parentFile.parentFile.canonicalFile)
        assertEquals("﻿" + text, copy.readText(Charsets.UTF_8))
        assertEquals(utf8, copies.copyOf(utf8))
    }

    @Test fun nothingIsCopiedAfterClear() {
        val parent = temp.newFolder("copies")
        val copies = SubtitleCopies(parent)
        val korean = file("Movie.smi", text.toByteArray(cp949))
        copies.prepare(korean)
        copies.clear()
        assertEquals(0, parent.listFiles().orEmpty().size)
        assertEquals(korean, copies.copyOf(korean))
        copies.prepare(file("Other.smi", text.toByteArray(cp949)))
        assertEquals("a prepare after the end of playback makes no copy", 0, parent.listFiles().orEmpty().size)
    }

    @Test fun thePreviousPlaybacksCleanupNeverRemovesTheNextOnesCopies() {
        val parent = temp.newFolder("copies")
        val previous = SubtitleCopies(parent, startedAtMs = 100)
        previous.prepare(file("First.smi", text.toByteArray(cp949)))
        val next = SubtitleCopies(parent, startedAtMs = 200)
        val second = file("Second.smi", text.toByteArray(cp949))
        val late = SubtitleCopies(parent, startedAtMs = 150)
        val pool = Executors.newFixedThreadPool(3)
        val start = CountDownLatch(1)
        try {
            pool.execute {
                start.await()
                previous.clear()
            }
            pool.execute {
                start.await()
                next.prepare(second)
            }
            // A previous playback whose first prepare runs late must not remove the newer folder either.
            pool.execute {
                start.await()
                late.prepare(file("Late.smi", text.toByteArray(cp949)))
            }
            start.countDown()
        } finally {
            pool.shutdown()
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS))
        }
        val copy = next.copyOf(second)
        assertNotEquals(second, copy)
        assertTrue("the next playback keeps its copy", copy.isFile)
        assertEquals("﻿" + text, copy.readText(Charsets.UTF_8))
    }
}
