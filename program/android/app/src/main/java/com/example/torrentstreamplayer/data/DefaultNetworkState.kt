package com.example.torrentstreamplayer.data

/**
 * Follows the default network from its callbacks. A late `onLost` for a network that is no longer the default (the
 * old Wi-Fi after mobile data took over) is ignored, so the connection is not left at [ConnectionType.NONE] while
 * another network is up. Pure and generic over the network handle so it is tested without Android.
 */
internal class DefaultNetworkState<N : Any> {
    private var current: N? = null

    /** The default network is [network] with [type]. Returns the connection to publish. */
    @Synchronized fun changed(network: N, type: ConnectionType): ConnectionType {
        current = network
        return type
    }

    /** [network] went away. Returns [ConnectionType.NONE] if it was the default network, otherwise null (no change). */
    @Synchronized fun lost(network: N): ConnectionType? {
        if (network != current) return null
        current = null
        return ConnectionType.NONE
    }
}
