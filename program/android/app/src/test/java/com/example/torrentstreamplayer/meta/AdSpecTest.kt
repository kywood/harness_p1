package com.example.torrentstreamplayer.meta

import org.junit.Assert.*
import org.junit.Test

class AdSpecTest {
    @Test fun completionRulesKeepPreviousValues() {
        assertEquals(300_000L, AdSpec.COMPLETION_MIN_WATCH_MS)
        assertEquals(600_000L, AdSpec.COMPLETION_COOLDOWN_MS)
        assertEquals(5, AdSpec.COMPLETION_COUNT)
    }

    @Test fun debugBuildsUseGoogleTestUnitsOnly() {
        AdUnit.entries.forEach {
            assertTrue(it.debugId.startsWith("ca-app-pub-3940256099942544/"))
            assertFalse(it.releaseId.startsWith("ca-app-pub-3940256099942544/"))
            assertEquals(it.debugId, it.id(true))
            assertEquals(it.releaseId, it.id(false))
        }
    }

    @Test fun unitIdsAreUnique() {
        val ids = AdUnit.entries.flatMap { listOf(it.debugId, it.releaseId) }
        assertEquals(ids.size, ids.toSet().size)
    }
}
