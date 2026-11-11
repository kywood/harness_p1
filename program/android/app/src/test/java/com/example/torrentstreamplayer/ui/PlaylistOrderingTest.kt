package com.example.torrentstreamplayer.ui
import com.example.torrentstreamplayer.engine.SavedMagnet
import org.junit.Assert.*
import org.junit.Test
class PlaylistOrderingTest {
    @Test fun selectedDownloadsExcludeCompletedAndUnselectedEntries() {
        assertEquals(listOf(recent, untouched), PlaylistOrdering.downloadTargets(listOf(old, recent, untouched), setOf("old", "recent", "new")))
        assertEquals(listOf(recent), PlaylistOrdering.downloadTargets(listOf(old, recent, untouched), setOf("recent")))
        assertTrue(PlaylistOrdering.downloadTargets(listOf(old, recent), emptySet()).isEmpty())
    }
    private val old = SavedMagnet("old", "Old", 10, 100, 100, lastPlayedAt = 300)
    private val recent = SavedMagnet("recent", "Recent", 20, 100, 25, lastPlayedAt = 200)
    private val untouched = SavedMagnet("new", "New", 30)
    @Test fun recentPlaybackSortIsIndependentFromReceivedDate() {
        assertEquals(listOf(old, recent, untouched), PlaylistOrdering.visible(listOf(untouched, recent, old), PlaylistSort.RECENT_PLAY, PlaylistFilter.ALL))
        assertEquals(listOf(untouched, recent, old), PlaylistOrdering.visible(listOf(old, recent, untouched), PlaylistSort.RECEIVED, PlaylistFilter.ALL))
    }
    @Test fun completeAndIncompleteFiltersIncludeUnknownOnlyAsIncomplete() {
        assertEquals(listOf(old), PlaylistOrdering.visible(listOf(old, recent, untouched), PlaylistSort.RECENT_PLAY, PlaylistFilter.COMPLETE))
        assertEquals(listOf(recent, untouched), PlaylistOrdering.visible(listOf(old, recent, untouched), PlaylistSort.RECENT_PLAY, PlaylistFilter.INCOMPLETE))
        assertFalse(PlaylistOrdering.complete(untouched.copy(fileSize = 0, downloadedBytes = 0)))
    }
    @Test fun neverPlayedEntriesUseReceivedDateAndLiveCompletionChangesFilter() {
        assertEquals(listOf(untouched, recent.copy(lastPlayedAt = 0)),
            PlaylistOrdering.visible(listOf(recent.copy(lastPlayedAt = 0), untouched), PlaylistSort.RECENT_PLAY, PlaylistFilter.ALL))
        assertTrue(PlaylistOrdering.visible(listOf(recent.copy(downloadedBytes = 100)), PlaylistSort.RECENT_PLAY, PlaylistFilter.INCOMPLETE).isEmpty())
    }
}
