package com.example.torrentstreamplayer.ui

import com.example.torrentstreamplayer.data.ConnectionType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WifiWaitPolicyTest {
    @Test fun showsOnlyWhenWifiOnlyIsOnAndConnectionIsMetered() {
        assertTrue(WifiWaitPolicy.waitsForWifi(wifiOnly = true, ConnectionType.MOBILE))
        assertTrue(WifiWaitPolicy.waitsForWifi(wifiOnly = true, ConnectionType.DATA_SAVER))
        assertFalse(WifiWaitPolicy.waitsForWifi(wifiOnly = true, ConnectionType.WIFI))
    }

    @Test fun hiddenWhenWifiOnlyIsOff() {
        ConnectionType.entries.forEach { assertFalse(WifiWaitPolicy.waitsForWifi(wifiOnly = false, it)) }
    }

    @Test fun hiddenWithoutAnyConnection() {
        assertFalse(WifiWaitPolicy.waitsForWifi(wifiOnly = true, ConnectionType.NONE))
    }
}
