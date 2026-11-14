package com.example.torrentstreamplayer.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DownloadOrderPreferenceTest {
    @Test fun aVideoWithoutItsOwnChoiceFollowsTheAppSetting() {
        DownloadOrderPreference.entries.forEach { appDefault ->
            assertEquals(appDefault, DownloadOrderPreference.effective(null, appDefault))
        }
    }

    @Test fun theVideosOwnChoiceWinsOverTheAppSetting() {
        DownloadOrderPreference.entries.forEach { perVideo ->
            DownloadOrderPreference.entries.forEach { appDefault ->
                assertEquals(perVideo, DownloadOrderPreference.effective(perVideo, appDefault))
            }
        }
    }

    @Test fun theDefaultIsFromTheStart() {
        assertEquals(DownloadOrderPreference.FROM_START, DownloadOrderPreference.DEFAULT)
    }

    @Test fun savedCodesAreStableUniqueAndRoundTrip() {
        assertEquals(1, DownloadOrderPreference.FROM_START.code)
        assertEquals(2, DownloadOrderPreference.FASTEST.code)
        DownloadOrderPreference.entries.forEach {
            assertEquals(it, DownloadOrderPreference.fromCode(DownloadOrderPreference.codeOf(it)))
        }
        assertEquals(DownloadOrderPreference.NO_CODE, DownloadOrderPreference.codeOf(null))
    }

    @Test fun noCodeAndUnknownCodesMeanNoChoice() {
        assertNull(DownloadOrderPreference.fromCode(DownloadOrderPreference.NO_CODE))
        assertNull(DownloadOrderPreference.fromCode(99))
        assertNull(DownloadOrderPreference.fromCode(-1))
    }
}
