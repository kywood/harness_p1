package com.example.torrentstreamplayer.data

import kotlinx.coroutines.flow.StateFlow

/**
 * The device's current internet connection as download rules see it. [metered] connections may cost the user
 * money (mobile data, metered Wi-Fi hotspots); [connected] is false when there is no internet at all.
 */
internal enum class ConnectionType(val connected: Boolean, val metered: Boolean) {
    /** Unmetered network: Wi-Fi or Ethernet. */
    WIFI(connected = true, metered = false),

    /** Mobile data or any metered network. */
    MOBILE(connected = true, metered = true),

    /** A metered network with Android's Data Saver on. */
    DATA_SAVER(connected = true, metered = true),

    NONE(connected = false, metered = false),
    ;

    companion object {
        /** Pure mapping from the network's capabilities, so it is tested without Android. */
        fun classify(connected: Boolean, unmetered: Boolean, dataSaver: Boolean): ConnectionType = when {
            !connected -> NONE
            unmetered -> WIFI
            dataSaver -> DATA_SAVER
            else -> MOBILE
        }
    }
}

/** Port for the connection watcher; the app uses [NetworkMonitor], tests pass a fixed flow. */
internal interface ConnectionSource {
    val connection: StateFlow<ConnectionType>

    /** Stops watching. Called once when the owner is cleared. */
    fun stop()
}
