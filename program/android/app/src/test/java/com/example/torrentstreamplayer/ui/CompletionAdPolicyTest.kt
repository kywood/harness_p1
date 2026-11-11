package com.example.torrentstreamplayer.ui

import org.junit.Assert.*
import org.junit.Test

class CompletionAdPolicyTest {
    @Test fun completionCountStaysDueUntilAnAdResetsIt() {
        var count = 0
        repeat(5) { count = CompletionAdPolicy.nextCompletionCount(count) }
        assertEquals(5, count)
        assertEquals(5, CompletionAdPolicy.nextCompletionCount(count))
        assertEquals(5, CompletionAdPolicy.nextCompletionCount(Int.MAX_VALUE))
        assertEquals(1, CompletionAdPolicy.nextCompletionCount(0))
    }
    @Test fun fifthShortCompletionQualifiesAndStillHonorsCooldown() {
        assertFalse(CompletionAdPolicy.eligible(1000, 900000, null, 4))
        assertTrue(CompletionAdPolicy.eligible(1000, 900000, null, 5))
        assertFalse(CompletionAdPolicy.eligible(1000, 899999, 300000, 5))
        assertTrue(CompletionAdPolicy.eligible(1000, 900000, 300000, 5))
    }
    @Test fun buildTypeSelectsCorrectUnit() {
        assertEquals("ca-app-pub-3940256099942544/1033173712", CompletionAdPolicy.unitId(true))
        assertEquals("ca-app-pub-2418522550325553/6999280238", CompletionAdPolicy.unitId(false))
    }
    @Test fun minimumWatchTimeAndCooldownAreRequired() {
        assertFalse(CompletionAdPolicy.eligible(299999, 900000, null))
        assertTrue(CompletionAdPolicy.eligible(300000, 900000, null))
        assertFalse(CompletionAdPolicy.eligible(300000, 899999, 300000))
        assertTrue(CompletionAdPolicy.eligible(300000, 900000, 300000))
        assertFalse(CompletionAdPolicy.eligible(300000, 200000, 300000))
    }
    @Test fun onlyContinuousForegroundPlaybackCounts() {
        val watch = PlaybackWatchTime()
        watch.sample(1000, 0, true)
        watch.sample(2000, 1000, true)
        watch.sample(3000, 1000, true)
        watch.sample(4000, 120000, true)
        assertEquals(1000L, watch.watchedMs)
        watch.sample(5000, 121000, true)
        assertEquals(2000L, watch.watchedMs)
    }
    @Test fun pauseBackgroundAndLongGapsAreExcluded() {
        val watch = PlaybackWatchTime()
        watch.sample(1000, 0, true)
        watch.sample(2000, 1000, false)
        watch.sample(3000, 2000, true)
        watch.sample(10000, 3000, true)
        assertEquals(0L, watch.watchedMs)
    }
}
