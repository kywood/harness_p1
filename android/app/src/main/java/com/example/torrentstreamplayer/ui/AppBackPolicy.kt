package com.example.torrentstreamplayer.ui

internal object AppBackPolicy {
    fun returnsToInput(state: StreamUiState, filesOpen: Boolean, historyOpen: Boolean): Boolean =
        !filesOpen && !historyOpen && state !is StreamUiState.Idle
}
