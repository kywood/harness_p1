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

class SubtitleCopiesTest {
    @get:Rule val temp = TemporaryFolder()
    private val text = "1\n00:00:01,000 --> 00:00:03,000\n한글 자막입니다.\n"

    private fun file(name: String, bytes: ByteArray) = File(temp.newFolder(), name).apply { writeBytes(bytes) }

    @Test fun leftoversGoBeforeTheFirstCopyAndOnlyCp949IsCopied() {
        val root = temp.newFolder("copies")
        val leftover = File(root, "0_old.smi").apply { writeText("old") }
        val copies = SubtitleCopies(root)
        val korean = file("Movie.smi", text.toByteArray(Charset.forName("x-windows-949")))
        val utf8 = file("Movie.srt", text.toByteArray(Charsets.UTF_8))
        copies.prepare(korean)
        copies.prepare(utf8)
        assertFalse(leftover.exists())
        val copy = copies.copyOf(korean)
        assertNotEquals(korean, copy)
        assertTrue(copy.canonicalPath.startsWith(root.canonicalPath))
        assertEquals("﻿" + text, copy.readText(Charsets.UTF_8))
        assertEquals(utf8, copies.copyOf(utf8))
    }

    @Test fun nothingIsCopiedAfterClear() {
        val root = temp.newFolder("copies")
        val copies = SubtitleCopies(root)
        val korean = file("Movie.smi", text.toByteArray(Charset.forName("x-windows-949")))
        copies.prepare(korean)
        copies.clear()
        assertFalse(root.exists())
        assertEquals(korean, copies.copyOf(korean))
        copies.prepare(file("Other.smi", text.toByteArray(Charset.forName("x-windows-949"))))
        assertFalse("a prepare after the end of playback makes no copy", root.exists())
    }
}
