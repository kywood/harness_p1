package com.example.torrentstreamplayer.ui
import androidx.annotation.StringRes
import com.example.torrentstreamplayer.R
import com.example.torrentstreamplayer.engine.SavedMagnet
enum class PlaylistSort(@StringRes val label: Int) { RECENT_PLAY(R.string.sort_recent_play), RECEIVED(R.string.sort_received) }
enum class PlaylistFilter(@StringRes val label: Int) {
    ALL(R.string.filter_all), COMPLETE(R.string.filter_complete), INCOMPLETE(R.string.filter_incomplete)
}
internal object PlaylistOrdering {
    fun downloadTargets(entries: List<SavedMagnet>, selected: Set<String>): List<SavedMagnet> =
        entries.filter { it.magnet in selected && !complete(it) }
    fun complete(entry: SavedMagnet): Boolean = entry.fileSize?.let { it > 0 && entry.downloadedBytes == it } == true
    fun visible(entries: List<SavedMagnet>, sort: PlaylistSort, filter: PlaylistFilter): List<SavedMagnet> =
        entries.filter { filter == PlaylistFilter.ALL || complete(it) == (filter == PlaylistFilter.COMPLETE) }
            .sortedWith(compareByDescending<SavedMagnet> { if (sort == PlaylistSort.RECENT_PLAY) it.lastPlayedAt else it.addedAt }
                .thenByDescending { it.addedAt }.thenBy { it.magnet })
}
