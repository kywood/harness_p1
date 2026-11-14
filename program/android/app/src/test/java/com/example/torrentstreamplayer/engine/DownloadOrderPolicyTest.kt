package com.example.torrentstreamplayer.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadOrderPolicyTest {
    @Test fun downloadOnlyTasksFetchFromTheFileStart() {
        val order = DownloadOrderPolicy.order(playbackFocused = false)
        assertEquals(DownloadOrder.FILE_START, order)
        assertTrue(order.sequential)
    }

    @Test fun playbackLeavesOrderToTheDeadlineWindow() {
        val order = DownloadOrderPolicy.order(playbackFocused = true)
        assertEquals(DownloadOrder.PLAYBACK_WINDOW, order)
        assertFalse(order.sequential)
    }

    @Test fun fromTheStartPreferenceKeepsDownloadOnlyTasksSequential() {
        val order = DownloadOrderPolicy.order(playbackFocused = false, DownloadOrderPreference.FROM_START)
        assertEquals(DownloadOrder.FILE_START, order)
        assertTrue(order.sequential)
    }

    @Test fun fastestPreferenceTurnsSequentialOffForDownloadOnlyTasks() {
        val order = DownloadOrderPolicy.order(playbackFocused = false, DownloadOrderPreference.FASTEST)
        assertEquals(DownloadOrder.RAREST_FIRST, order)
        assertFalse(order.sequential)
    }

    @Test fun playbackIgnoresThePreference() {
        DownloadOrderPreference.entries.forEach {
            assertEquals(DownloadOrder.PLAYBACK_WINDOW, DownloadOrderPolicy.order(playbackFocused = true, it))
        }
    }

    @Test fun edgePiecesAreTheFirstAndLastTwo() {
        assertEquals(setOf(10, 11, 98, 99), DownloadOrderPolicy.edgePieces(10, 99))
    }

    @Test fun edgePiecesStayInsideShortFiles() {
        assertEquals(setOf(5), DownloadOrderPolicy.edgePieces(5, 5))
        assertEquals(setOf(5, 6), DownloadOrderPolicy.edgePieces(5, 6))
        assertEquals(emptySet<Int>(), DownloadOrderPolicy.edgePieces(-1, -1))
        assertEquals(emptySet<Int>(), DownloadOrderPolicy.edgePieces(7, 6))
    }
}
