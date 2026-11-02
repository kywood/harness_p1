package com.example.torrentstreamplayer.ui

internal object TestAdPolicy {
    const val BANNER_UNIT_ID = "ca-app-pub-3940256099942544/9214589741"
    fun visible(state: StreamUiState, files: Boolean, playlist: Boolean): Boolean =
        !files && (playlist || state is StreamUiState.Idle || state is StreamUiState.Error)
}
