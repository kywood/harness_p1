package com.example.torrentstreamplayer.engine

import org.junit.Assert.*
import org.junit.Test

class MetadataStartupTest {
    @Test fun networkFetchStartsOnlyAfterSessionHasSettled() {
        val events = mutableListOf<String>()
        val result = MetadataStartup.load(null, { events.add("start"); "session" },
            { events.add("settle") }) { assertEquals("session", it); events.add("fetch"); byteArrayOf(7) }
        assertEquals(listOf("start", "settle", "fetch"), events)
        assertArrayEquals(byteArrayOf(7), result.first)
        assertEquals("session", result.second)
    }
    @Test fun completedCacheLookupDoesNotInitializeNetwork() {
        val bytes = byteArrayOf(8)
        val result = MetadataStartup.load(bytes, { error("Network must remain stopped") },
            { error("No startup delay for cache") }) { error("No metadata fetch for cache") }
        assertArrayEquals(bytes, result.first)
        assertNull(result.second)
    }
    @Test fun metadataTimeoutIsReturnedWithoutRepeatingFetch() {
        var fetches = 0
        val result = MetadataStartup.load(null, { "session" }, {}) { fetches++; null }
        assertNull(result.first)
        assertEquals(1, fetches)
    }
    @Test fun sessionStartupFailureIsNotHiddenOrRetried() {
        assertThrows(IllegalStateException::class.java) {
            MetadataStartup.load(null, { error("Session failed") }, { fail("Must not settle") }) {
                fail("Must not fetch"); null
            }
        }
    }
}
