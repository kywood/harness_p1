package com.example.torrentstreamplayer.ui

import androidx.annotation.StringRes
import com.example.torrentstreamplayer.R

internal object DownloadSelection {
    fun stoppable(selected: Set<String>, jobs: Map<String, DownloadEntry>, legacyKey: String? = null): Set<String> =
        selected.filter { key -> key == legacyKey || jobs[key]?.phase == DownloadPhase.RUNNING ||
            jobs[key]?.phase == DownloadPhase.QUEUED }.toSet()
    @StringRes fun label(stoppable: Set<String>): Int =
        if (stoppable.isEmpty()) R.string.action_download else R.string.action_stop
}
