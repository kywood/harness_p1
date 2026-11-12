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
 * Adapter from Android's default network callback to [ConnectionSource], with the existing `ACCESS_NETWORK_STATE`
 * permission. [watch] creates and starts one; the owner calls [stop] when it is cleared. Callbacks arrive on a
 * system thread; [connection] is safe to read anywhere.
 */
internal class NetworkMonitor private constructor(context: Context) : ConnectionSource {
    private val manager = context.getSystemService(ConnectivityManager::class.java)
    private val mutableConnection = MutableStateFlow(current())
    override val connection: StateFlow<ConnectionType> = mutableConnection.asStateFlow()
    private val defaultNetwork = DefaultNetworkState<Network>()

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            mutableConnection.value = defaultNetwork.changed(network, classify(capabilities))
        }

        override fun onLost(network: Network) {
            defaultNetwork.lost(network)?.let { mutableConnection.value = it }
        }
    }

    private fun start() {
        runCatching { manager?.registerDefaultNetworkCallback(callback) }
            .onFailure { Log.w(TAG, "Unable to watch network: ${EngineFailureLog.describe(it)}") }
    }

    override fun stop() {
        runCatching { manager?.unregisterNetworkCallback(callback) }
    }

    private fun current(): ConnectionType =
        runCatching { classify(manager?.getNetworkCapabilities(manager.activeNetwork)) }.getOrDefault(ConnectionType.NONE)

    private fun classify(capabilities: NetworkCapabilities?): ConnectionType = ConnectionType.classify(
        connected = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true,
        unmetered = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) == true,
        dataSaver = manager?.restrictBackgroundStatus == ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED,
    )

    companion object {
        private const val TAG = "NetworkMonitor"

        /** Creates a monitor and starts watching. */
        fun watch(context: Context): ConnectionSource = NetworkMonitor(context).also { it.start() }
    }
}
