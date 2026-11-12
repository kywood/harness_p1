package com.example.torrentstreamplayer.ui

import com.example.torrentstreamplayer.data.ConnectionType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsNoticePolicyTest {
    @Test fun showsOnlyWhenWifiOnlyIsOnAndConnectionIsMetered() {
        assertTrue(SettingsNoticePolicy.showsWifiPaused(wifiOnly = true, ConnectionType.MOBILE))
        assertTrue(SettingsNoticePolicy.showsWifiPaused(wifiOnly = true, ConnectionType.DATA_SAVER))
        assertFalse(SettingsNoticePolicy.showsWifiPaused(wifiOnly = true, ConnectionType.WIFI))
    }

    @Test fun hiddenWhenWifiOnlyIsOff() {
        ConnectionType.entries.forEach { assertFalse(SettingsNoticePolicy.showsWifiPaused(wifiOnly = false, it)) }
    }

    @Test fun hiddenWithoutAnyConnection() {
        assertFalse(SettingsNoticePolicy.showsWifiPaused(wifiOnly = true, ConnectionType.NONE))
    }
}
