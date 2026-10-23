package com.example.torrentstreamplayer.ui

sealed interface StreamUiState {
    data object Idle : StreamUiState
    data class FetchingMetadata(val magnetUrl: String) : StreamUiState
    data class Buffering(
        val fileName: String,
        val totalSize: Long,
        val progress: Float,
        val downloadSpeedBytes: Long
    ) : StreamUiState
    data class Playing(
        val fileName: String
    ) : StreamUiState
    data class Error(val message: String) : StreamUiState
}
