package com.example.torrentstreamplayer

import com.example.torrentstreamplayer.ui.StreamUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamUiStateTest {

    @Test
    fun testIdleState() {
        val state: StreamUiState = StreamUiState.Idle
        assertTrue(state is StreamUiState.Idle)
    }

    @Test
    fun testBufferingStateProgress() {
        val bufferingState = StreamUiState.Buffering(
            fileName = "sample_video.mkv",
            totalSize = 1024L * 1024L * 500L,
            progress = 5.5f,
            downloadSpeedBytes = 1024L * 1000L
        )

        assertEquals("sample_video.mkv", bufferingState.fileName)
        assertEquals(5.5f, bufferingState.progress, 0.01f)
    }

    @Test
    fun testPlayingState() {
        val playingState = StreamUiState.Playing(
            fileName = "sample_video.mkv"
        )

        assertEquals("sample_video.mkv", playingState.fileName)
    }
}
