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

    @Test fun playbackAsksOnlyWithWifiOnlyOnMeteredNetworks() {
        fun check(wifiOnly: Boolean, connection: ConnectionType, allowedOnce: Boolean = false, complete: Boolean = false) =
            DownloadNetworkPolicy.playbackStart(NetworkConditions(wifiOnly, connection), allowedOnce, complete)
        ConnectionType.entries.forEach {
            assertEquals("no first mobile-data notice without Wi-Fi only", PlaybackNetworkCheck.START, check(false, it))
        }
        assertEquals(PlaybackNetworkCheck.START, check(true, ConnectionType.WIFI))
        assertEquals(PlaybackNetworkCheck.START, check(true, ConnectionType.NONE))
        assertEquals(PlaybackNetworkCheck.CONFIRM_MOBILE, check(true, ConnectionType.MOBILE))
        assertEquals(PlaybackNetworkCheck.CONFIRM_MOBILE, check(true, ConnectionType.DATA_SAVER))
        assertEquals(PlaybackNetworkCheck.START, check(true, ConnectionType.MOBILE, allowedOnce = true))
        assertEquals("a finished file uses no data", PlaybackNetworkCheck.START,
            check(true, ConnectionType.MOBILE, complete = true))
    }

    @Test fun noConnectionKeepsTheLastConnectionButTakesNewSettings() {
        val wifi = NetworkConditions(wifiOnly = true, connection = ConnectionType.WIFI)
        assertEquals(wifi, DownloadNetworkPolicy.settle(wifi, NetworkConditions(true, ConnectionType.NONE)))
        assertEquals(NetworkConditions(false, ConnectionType.WIFI),
            DownloadNetworkPolicy.settle(wifi, NetworkConditions(false, ConnectionType.NONE)))
        assertEquals(NetworkConditions(true, ConnectionType.MOBILE),
            DownloadNetworkPolicy.settle(wifi, NetworkConditions(true, ConnectionType.MOBILE)))
    }

    @Test fun playbackOnMobileDataIsReportedOnlyWhenWifiOnlyWasNotWaived() {
        fun plays(wifiOnly: Boolean, connection: ConnectionType, allowedOnce: Boolean = false) =
            DownloadNetworkPolicy.playsOnMobileData(NetworkConditions(wifiOnly, connection), allowedOnce)
        assertEquals(true, plays(true, ConnectionType.MOBILE))
        assertEquals(true, plays(true, ConnectionType.DATA_SAVER))
        assertEquals(false, plays(true, ConnectionType.WIFI))
        assertEquals(false, plays(false, ConnectionType.MOBILE))
        assertEquals(false, plays(true, ConnectionType.MOBILE, allowedOnce = true))
    }
}
