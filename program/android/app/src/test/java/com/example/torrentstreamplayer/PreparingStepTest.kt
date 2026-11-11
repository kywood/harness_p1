package com.example.torrentstreamplayer

import com.example.torrentstreamplayer.ui.StreamUiState
import com.example.torrentstreamplayer.ui.navigation.AppScreen
import com.example.torrentstreamplayer.ui.screens.PreparingStep
import org.junit.Assert.assertEquals
import org.junit.Test

class PreparingStepTest {
    @Test fun beforeMetadataTheInfoStepIsActive() {
        assertEquals(PreparingStep.INFO, PreparingStep.of(AppScreen.Preparing.FetchingMetadata, hasMetadata = false))
    }

    @Test fun afterMetadataTheConnectStepIsActive() {
        assertEquals(PreparingStep.CONNECT, PreparingStep.of(AppScreen.Preparing.FetchingMetadata, hasMetadata = true))
    }

    @Test fun bufferingShowsTheLastStepRegardlessOfMetadata() {
        val buffering = AppScreen.Preparing.Buffering(StreamUiState.Buffering("Sample", 1000, 10f, 0))
        assertEquals(PreparingStep.BUFFERING, PreparingStep.of(buffering, hasMetadata = false))
        assertEquals(PreparingStep.BUFFERING, PreparingStep.of(buffering, hasMetadata = true))
    }

    @Test fun stepsAreShownInInfoConnectBufferingOrder() {
        assertEquals(listOf(PreparingStep.INFO, PreparingStep.CONNECT, PreparingStep.BUFFERING), PreparingStep.entries)
    }
}
