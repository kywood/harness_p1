package com.example.torrentstreamplayer.ui

import com.example.torrentstreamplayer.data.ConnectionType
import com.example.torrentstreamplayer.ui.navigation.PlaybackOrigin

/** What a download may do on the current connection. */
internal enum class NetworkDecision(val mayRun: Boolean) {
    PROCEED(mayRun = true),

    /** "Wi-Fi 대기": stopped until an unmetered network returns. */
    WAIT_FOR_WIFI(mayRun = false),

    /** Wi-Fi only is on, but the user allowed mobile data for this item this time. */
    ALLOW_ONCE(mayRun = true),
}

/** What to do before starting playback on the current connection. */
internal enum class PlaybackNetworkCheck {
    START,

    /** Wi-Fi only is on and the connection is metered: ask "이번만 재생" first. */
    CONFIRM_MOBILE,
}

/** Playback held for the mobile-data confirmation: the resolved [magnet] and where playback was started. */
internal data class MobilePlaybackRequest(val magnet: String, val origin: PlaybackOrigin)

/** Network facts that download decisions depend on. Changing together, so replaced as one value. */
internal data class NetworkConditions(val wifiOnly: Boolean = false, val connection: ConnectionType = ConnectionType.WIFI)

/** Wi-Fi only rules. Pure: settings, connection and the user's one-time permission come in as values. */
internal object DownloadNetworkPolicy {
    /**
     * Conditions to act on after [next] arrives. No connection decides nothing (libtorrent idles by itself), so it
     * keeps [previous]'s connection while still taking the new Wi-Fi only setting.
     */
    fun settle(previous: NetworkConditions, next: NetworkConditions): NetworkConditions =
        if (next.connection.connected) next else next.copy(connection = previous.connection)

    fun decide(conditions: NetworkConditions, allowedOnce: Boolean): NetworkDecision = when {
        !conditions.wifiOnly -> NetworkDecision.PROCEED
        conditions.connection.connected && !conditions.connection.metered -> NetworkDecision.PROCEED
        allowedOnce && conditions.connection.connected -> NetworkDecision.ALLOW_ONCE
        else -> NetworkDecision.WAIT_FOR_WIFI
    }

    /**
     * Before playback starts: ask only while Wi-Fi only is on and the connection is metered. A finished file uses no
     * data and an item already allowed once is not asked again. No connection starts (the offline error shows there).
     */
    fun playbackStart(conditions: NetworkConditions, allowedOnce: Boolean, complete: Boolean): PlaybackNetworkCheck =
        when {
            complete || allowedOnce -> PlaybackNetworkCheck.START
            conditions.wifiOnly && conditions.connection.metered -> PlaybackNetworkCheck.CONFIRM_MOBILE
            else -> PlaybackNetworkCheck.START
        }

    /**
     * Playback that keeps going on mobile data although Wi-Fi only is on (Wi-Fi dropped while watching). Playback is
     * never stopped for it; the player shows a "모바일 데이터로 재생 중" notice instead.
     */
    fun playsOnMobileData(conditions: NetworkConditions, allowedOnce: Boolean): Boolean =
        conditions.wifiOnly && conditions.connection.metered && !allowedOnce
}
