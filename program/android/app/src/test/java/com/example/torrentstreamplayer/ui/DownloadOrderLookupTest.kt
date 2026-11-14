package com.example.torrentstreamplayer.ui

import com.example.torrentstreamplayer.engine.DownloadOrderPreference
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class DownloadOrderLookupTest {
    private val first = "a".repeat(64)
    private val second = "b".repeat(64)

    @Test fun theVideosOwnValueWinsAndOthersFollowTheAppDefault() {
        val lookup = DownloadOrderLookup({ mapOf(first to DownloadOrderPreference.FASTEST) },
            { DownloadOrderPreference.FROM_START })
        assertEquals(DownloadOrderPreference.FASTEST, lookup.orderFor(first))
        assertEquals(DownloadOrderPreference.FROM_START, lookup.orderFor(second))
    }

    @Test fun aChangedAppDefaultIsReadOnEveryLookup() {
        var appDefault = DownloadOrderPreference.FROM_START
        val lookup = DownloadOrderLookup({ mapOf(first to DownloadOrderPreference.FROM_START) }, { appDefault })
        assertEquals(DownloadOrderPreference.FROM_START, lookup.orderFor(second))
        appDefault = DownloadOrderPreference.FASTEST
        assertEquals(DownloadOrderPreference.FASTEST, lookup.orderFor(second))
        assertEquals("the video's own value is not touched", DownloadOrderPreference.FROM_START, lookup.orderFor(first))
    }

    @Test fun severalTasksReadTheSavedValuesOnce() {
        var reads = 0
        val lookup = DownloadOrderLookup({
            reads++
            mapOf(second to DownloadOrderPreference.FASTEST)
        }, { DownloadOrderPreference.FROM_START })
        assertEquals(mapOf(first to DownloadOrderPreference.FROM_START, second to DownloadOrderPreference.FASTEST),
            lookup.ordersFor(listOf(first, second)))
        assertEquals(1, reads)
    }

    @Test fun noTasksReadNothing() {
        val lookup = DownloadOrderLookup({ error("must not read the playlist") }, { error("must not read settings") })
        assertEquals(emptyMap<String, DownloadOrderPreference>(), lookup.ordersFor(emptyList()))
    }

    @Test fun anUnreadablePlaylistFallsBackToTheAppDefault() {
        val lookup = DownloadOrderLookup({ throw IOException("read failed") }, { DownloadOrderPreference.FASTEST })
        assertEquals(DownloadOrderPreference.FASTEST, lookup.orderFor(first))
    }
}
