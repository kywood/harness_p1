package com.example.torrentstreamplayer.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class LicenseParagraphsTest {
    @Test fun blankLinesSeparateParagraphsAndInnerLineBreaksStay() {
        val text = "  TITLE\n  Version 2\n\n\nFirst line\nsecond line\n   \n  indented\n"
        assertEquals(listOf("  TITLE\n  Version 2", "First line\nsecond line", "  indented"), LicenseParagraphs.split(text))
    }

    @Test fun windowsLineEndingsAndEmptyTextAreHandled() {
        assertEquals(listOf("a\nb", "c"), LicenseParagraphs.split("a\r\nb\r\n\r\nc\r\n"))
        assertEquals(emptyList<String>(), LicenseParagraphs.split("\n \n"))
    }
}
