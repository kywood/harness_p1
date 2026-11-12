package com.example.torrentstreamplayer.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MagnetDisplayNameTest {
    private val hash = "1".repeat(40)

    @Test fun decodesTheDnParameter() {
        assertEquals("My Movie", MagnetDisplayName.of("magnet:?xt=urn:btih:$hash&dn=My+Movie"))
        assertEquals("영화", MagnetDisplayName.of("  magnet:?dn=%EC%98%81%ED%99%94&xt=urn:btih:$hash "))
    }

    @Test fun missingBlankOrBrokenNamesAreNull() {
        assertNull(MagnetDisplayName.of("magnet:?xt=urn:btih:$hash"))
        assertNull(MagnetDisplayName.of("magnet:?xt=urn:btih:$hash&dn=+"))
        assertNull(MagnetDisplayName.of("magnet:?xt=urn:btih:$hash&dn=%E"))
    }

    @Test fun longNamesAreCut() {
        val name = MagnetDisplayName.of("magnet:?xt=urn:btih:$hash&dn=" + "a".repeat(500))
        assertEquals(MagnetDisplayName.MAX_CHARS, name?.length)
    }
}
