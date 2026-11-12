package com.example.torrentstreamplayer.ui

import com.example.torrentstreamplayer.data.ConnectionType
import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadNetworkPolicyTest {
    private fun decide(wifiOnly: Boolean, connection: ConnectionType, allowedOnce: Boolean = false) =
        DownloadNetworkPolicy.decide(NetworkConditions(wifiOnly, connection), allowedOnce)

    @Test fun connectionTypesFollowCapabilities() {
        assertEquals(ConnectionType.NONE, ConnectionType.classify(connected = false, unmetered = true, dataSaver = true))
        assertEquals(ConnectionType.WIFI, ConnectionType.classify(connected = true, unmetered = true, dataSaver = true))
        assertEquals(ConnectionType.DATA_SAVER,
            ConnectionType.classify(connected = true, unmetered = false, dataSaver = true))
        assertEquals(ConnectionType.MOBILE, ConnectionType.classify(connected = true, unmetered = false, dataSaver = false))
    }

    @Test fun withoutWifiOnlyEverythingProceeds() {
        ConnectionType.entries.forEach { assertEquals(NetworkDecision.PROCEED, decide(false, it)) }
    }

    @Test fun wifiOnlyWaitsOnMeteredOrMissingNetworksUnlessAllowedOnce() {
        assertEquals(NetworkDecision.PROCEED, decide(true, ConnectionType.WIFI))
        assertEquals(NetworkDecision.WAIT_FOR_WIFI, decide(true, ConnectionType.MOBILE))
        assertEquals(NetworkDecision.WAIT_FOR_WIFI, decide(true, ConnectionType.DATA_SAVER))
        assertEquals(NetworkDecision.WAIT_FOR_WIFI, decide(true, ConnectionType.NONE))
        assertEquals(NetworkDecision.ALLOW_ONCE, decide(true, ConnectionType.MOBILE, allowedOnce = true))
        assertEquals(NetworkDecision.ALLOW_ONCE, decide(true, ConnectionType.DATA_SAVER, allowedOnce = true))
        assertEquals(NetworkDecision.WAIT_FOR_WIFI, decide(true, ConnectionType.NONE, allowedOnce = true))
        assertEquals(NetworkDecision.PROCEED, decide(true, ConnectionType.WIFI, allowedOnce = true))
    }

    @Test fun playbackAsksOnMeteredNetworksOnlyWhenNeeded() {
        fun check(wifiOnly: Boolean, connection: ConnectionType, seen: Boolean) =
            DownloadNetworkPolicy.playbackStart(NetworkConditions(wifiOnly, connection), seen)
        assertEquals(PlaybackNetworkCheck.START, check(true, ConnectionType.WIFI, seen = false))
        assertEquals(PlaybackNetworkCheck.START, check(false, ConnectionType.NONE, seen = false))
        assertEquals(PlaybackNetworkCheck.CONFIRM_MOBILE, check(false, ConnectionType.MOBILE, seen = false))
        assertEquals(PlaybackNetworkCheck.START, check(false, ConnectionType.MOBILE, seen = true))
        assertEquals(PlaybackNetworkCheck.CONFIRM_MOBILE, check(true, ConnectionType.MOBILE, seen = true))
        assertEquals(PlaybackNetworkCheck.CONFIRM_MOBILE, check(true, ConnectionType.DATA_SAVER, seen = true))
    }
}
