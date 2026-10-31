package com.example.torrentstreamplayer.ui
import com.example.torrentstreamplayer.engine.SavedMagnet
enum class PlaylistSort(val label: String) { RECENT_PLAY("최근 재생순"), RECEIVED("받은 날짜순") }
enum class PlaylistFilter(val label: String) { ALL("모두"), COMPLETE("다운로드 완료"), INCOMPLETE("다운로드 미완료") }
internal object PlaylistOrdering {
    fun downloadTargets(entries: List<SavedMagnet>, selected: Set<String>): List<SavedMagnet> =
        entries.filter { it.magnet in selected && !complete(it) }
    fun complete(entry: SavedMagnet): Boolean = entry.fileSize?.let { it > 0 && entry.downloadedBytes == it } == true
    fun visible(entries: List<SavedMagnet>, sort: PlaylistSort, filter: PlaylistFilter): List<SavedMagnet> =
        entries.filter { filter == PlaylistFilter.ALL || complete(it) == (filter == PlaylistFilter.COMPLETE) }
            .sortedWith(compareByDescending<SavedMagnet> { if (sort == PlaylistSort.RECENT_PLAY) it.lastPlayedAt else it.addedAt }
                .thenByDescending { it.addedAt }.thenBy { it.magnet })
}
