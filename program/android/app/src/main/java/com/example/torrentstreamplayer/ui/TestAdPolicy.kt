package com.example.torrentstreamplayer.ui

import com.example.torrentstreamplayer.meta.AdUnit

internal object TestAdPolicy {
    /** Google's test banner unit; kept as a name for existing callers. */
    val BANNER_UNIT_ID: String get() = AdUnit.BANNER.debugId
    fun bannerUnitId(debug: Boolean): String = AdUnit.BANNER.id(debug)
    fun visible(state: StreamUiState, files: Boolean, playlist: Boolean): Boolean =
        !files && (playlist || state is StreamUiState.Idle || state is StreamUiState.Error)
}
