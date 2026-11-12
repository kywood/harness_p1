package com.example.torrentstreamplayer.ui

import com.example.torrentstreamplayer.data.ConnectionType

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

    /** Ask first (mobile-data notice; the user may allow this once or keep waiting for Wi-Fi). */
    CONFIRM_MOBILE,
}

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
     * Before "바로 재생": on a metered connection, ask every time while Wi-Fi only is on, and otherwise only until
     * the user has seen the mobile-data notice once. Unmetered or no connection starts without asking.
     */
    fun playbackStart(conditions: NetworkConditions, mobileNoticeSeen: Boolean): PlaybackNetworkCheck = when {
        !conditions.connection.metered -> PlaybackNetworkCheck.START
        conditions.wifiOnly -> PlaybackNetworkCheck.CONFIRM_MOBILE
        mobileNoticeSeen -> PlaybackNetworkCheck.START
        else -> PlaybackNetworkCheck.CONFIRM_MOBILE
    }
}
