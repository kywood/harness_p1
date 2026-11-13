package com.example.torrentstreamplayer

import com.example.torrentstreamplayer.data.ConnectionSource
import com.example.torrentstreamplayer.data.ConnectionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * A connection that never changes. Tests that build [com.example.torrentstreamplayer.ui.MainViewModel] on a bare
 * `Application` (no base context) pass it, since the default `NetworkMonitor` needs a real context.
 */
internal class FixedConnection(type: ConnectionType = ConnectionType.WIFI) : ConnectionSource {
    override val connection: StateFlow<ConnectionType> = MutableStateFlow(type)
    override fun stop() = Unit
}
