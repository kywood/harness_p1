package com.example.torrentstreamplayer.engine

import org.junit.Assert.*
import org.junit.Test

class MagnetIdentityTest {
    private fun link(hash: String) = "magnet:?xt=urn:btih:" + hash
    @Test fun trackerAndDisplayNameChangesDoNotLoseCachedDownload() {
        val original = link("ab".repeat(20))
        assertEquals(MagnetIdentity.key(original + "&dn=Movie&tr=https%3A%2F%2Fone.test"),
            MagnetIdentity.key(original + "&tr=https%3A%2F%2Ftwo.test&dn=Other"))
    }
    @Test fun base32AndHexAndCaseUseSameCache() {
        assertEquals(MagnetIdentity.key(link("0".repeat(40))),
            MagnetIdentity.key(link("A".repeat(32))))
        assertEquals(MagnetIdentity.key(link("ab".repeat(20))),
            MagnetIdentity.key(link("AB".repeat(20))))
    }
    @Test fun differentTorrentsWithSameFilenameStaySeparate() {
        assertNotEquals(MagnetIdentity.key(link("a".repeat(40)) + "&dn=video.mp4"),
            MagnetIdentity.key(link("b".repeat(40)) + "&dn=video.mp4"))
    }
    @Test fun invalidInputsCannotBecomeDiskPaths() {
        listOf("", "https://example.test", link("../bad"), link("a".repeat(39))).forEach {
            assertThrows(Exception::class.java) { MagnetIdentity.key(it) }
        }
        assertTrue(MagnetIdentity.key(link("c".repeat(40))).matches(Regex("[0-9a-f]{64}")))
    }
    @Test fun v2InfoHashIsAccepted() {
        assertEquals(64, MagnetIdentity.key("magnet:?xt=urn:btmh:1220" + "a".repeat(64)).length)
    }
}
