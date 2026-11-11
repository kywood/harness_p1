package com.example.torrentstreamplayer.engine

import org.junit.Assert.*
import org.junit.Test

class DownloadOnlyPolicyTest {
    @Test fun replacesUnlimitedUploadsAndFastExceptionsWithoutLimitingDownloads() {
        val settings = mutableMapOf("slots" to -1, "fast" to 5, "choker" to 2, "downloadLimit" to 0)
        settings.putAll(DownloadOnlyPolicy.settings("choker", "slots", "fast", 0))
        assertEquals(0, settings["slots"])
        assertEquals(0, settings["fast"])
        assertEquals(0, settings["choker"])
        assertEquals(0, settings["downloadLimit"])
    }
    @Test fun preservesNativeFixedChokerValueRatherThanAssumingItsOrdinal() {
        val settings = DownloadOnlyPolicy.settings(101, 102, 103, 27)
        assertEquals(27, settings[101])
        assertEquals(0, settings[102])
        assertEquals(0, settings[103])
    }
    @Test fun ipv4AndIpv6PeersUseGlobalClassWithoutLocalUploadExemption() {
        val rules = mutableListOf<Triple<String, String, Long>>()
        DownloadOnlyPolicy.assignPeerClasses { first, last, mask -> rules.add(Triple(first, last, mask)) }
        assertEquals(2, rules.size)
        assertEquals("0.0.0.0", rules[0].first)
        assertEquals("255.255.255.255", rules[0].second)
        assertEquals("::", rules[1].first)
        assertEquals("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff", rules[1].second)
        assertTrue(rules.all { it.third == 1L })
    }
    @Test fun failureToInstallUploadProtectionIsPropagated() {
        assertThrows(IllegalStateException::class.java) {
            DownloadOnlyPolicy.assignPeerClasses { _, _, _ -> error("Cannot install peer policy") }
        }
    }
}
