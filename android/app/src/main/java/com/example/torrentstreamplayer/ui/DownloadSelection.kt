package com.example.torrentstreamplayer.ui
internal object DownloadSelection {
    fun stoppable(selected: Set<String>, jobs: Map<String, DownloadEntry>, legacyKey: String? = null): Set<String> =
        selected.filter { key -> key == legacyKey || jobs[key]?.phase == DownloadPhase.RUNNING ||
            jobs[key]?.phase == DownloadPhase.QUEUED }.toSet()
    fun label(stoppable: Set<String>): String = if (stoppable.isEmpty()) "다운로드" else "중지"
}
