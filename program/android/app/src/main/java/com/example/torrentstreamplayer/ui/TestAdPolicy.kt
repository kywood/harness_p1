package com.example.torrentstreamplayer.ui

internal object TestAdPolicy {
    const val BANNER_UNIT_ID = "ca-app-pub-3940256099942544/9214589741"
    private const val PRODUCTION_BANNER_UNIT_ID = "ca-app-pub-2418522550325553/6125182266"
    fun bannerUnitId(debug: Boolean): String =
        if (debug) BANNER_UNIT_ID else PRODUCTION_BANNER_UNIT_ID
    fun visible(state: StreamUiState, files: Boolean, playlist: Boolean): Boolean =
        !files && (playlist || state is StreamUiState.Idle || state is StreamUiState.Error)
}
