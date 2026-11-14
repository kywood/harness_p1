package com.example.torrentstreamplayer.ui

import com.example.torrentstreamplayer.engine.DownloadOrderPreference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadOrderOptionTest {
    @Test fun everySavedValueHasExactlyOneOption() {
        DownloadOrderPreference.entries.forEach { preference ->
            assertEquals(preference, DownloadOrderOption.of(preference).preference)
            assertEquals(1, DownloadOrderOption.entries.count { it.preference == preference })
        }
        assertEquals(DownloadOrderOption.USE_DEFAULT, DownloadOrderOption.of(null))
    }

    @Test fun settingsOffersTheTwoOrdersWithDescriptionsAndTheDefaultFirst() {
        assertEquals(listOf(DownloadOrderOption.FROM_START, DownloadOrderOption.FASTEST), DownloadOrderOption.forSettings)
        assertEquals(DownloadOrderPreference.DEFAULT, DownloadOrderOption.forSettings.first().preference)
        DownloadOrderOption.forSettings.forEach { assertNotNull(it.description) }
    }

    @Test fun aVideoOffersUseDefaultFirstThenTheTwoOrders() {
        assertEquals(listOf(DownloadOrderOption.USE_DEFAULT, DownloadOrderOption.FROM_START, DownloadOrderOption.FASTEST),
            DownloadOrderOption.forVideo)
    }

    @Test fun anUnfinishedVideoShowsTheActionAndNoMarkerWhileItFollowsTheDefault() {
        val state = DownloadOrderCardState.of(saved = null, complete = false)
        assertTrue(state.showsAction)
        assertNull(state.marker)
    }

    @Test fun anUnfinishedVideoWithItsOwnOrderShowsThatOrderAsTheMarker() {
        assertEquals(DownloadOrderOption.FASTEST,
            DownloadOrderCardState.of(DownloadOrderPreference.FASTEST, complete = false).marker)
        assertEquals(DownloadOrderOption.FROM_START,
            DownloadOrderCardState.of(DownloadOrderPreference.FROM_START, complete = false).marker)
    }

    @Test fun aCompleteVideoShowsNeitherTheActionNorTheMarker() {
        (DownloadOrderPreference.entries + null).forEach { saved ->
            val state = DownloadOrderCardState.of(saved, complete = true)
            assertFalse(state.showsAction)
            assertNull(state.marker)
        }
    }
}
