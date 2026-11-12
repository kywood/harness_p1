package com.example.torrentstreamplayer.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import com.example.torrentstreamplayer.engine.EngineFailureLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Watches the default network with the existing `ACCESS_NETWORK_STATE` permission. Call [start] once and [stop]
 * when the owner is cleared. Callbacks arrive on a system thread; [connection] is safe to read anywhere.
 */
internal class NetworkMonitor(context: Context) : ConnectionSource {
    private val manager = context.getSystemService(ConnectivityManager::class.java)
    private val mutableConnection = MutableStateFlow(current())
    override val connection: StateFlow<ConnectionType> = mutableConnection.asStateFlow()

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            mutableConnection.value = classify(capabilities)
        }

        override fun onLost(network: Network) {
            mutableConnection.value = ConnectionType.NONE
        }
    }

    fun start() {
        runCatching { manager?.registerDefaultNetworkCallback(callback) }
            .onFailure { Log.w(TAG, "Unable to watch network: ${EngineFailureLog.describe(it)}") }
    }

    fun stop() {
        runCatching { manager?.unregisterNetworkCallback(callback) }
    }

    private fun current(): ConnectionType =
        runCatching { classify(manager?.getNetworkCapabilities(manager.activeNetwork)) }.getOrDefault(ConnectionType.NONE)

    private fun classify(capabilities: NetworkCapabilities?): ConnectionType = ConnectionType.classify(
        connected = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true,
        unmetered = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) == true,
        dataSaver = manager?.restrictBackgroundStatus == ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED,
    )

    private companion object {
        const val TAG = "NetworkMonitor"
    }
}
