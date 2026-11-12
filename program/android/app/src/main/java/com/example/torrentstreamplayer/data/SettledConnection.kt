package com.example.torrentstreamplayer.data

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

/**
 * The connection once it has stayed the same for [settleMs]. A connection that flips for a moment (Wi-Fi → mobile →
 * Wi-Fi) then does not stop and restart downloads. The first value passes at once so startup is not delayed.
 */
@OptIn(FlowPreview::class)
internal fun Flow<ConnectionType>.settled(settleMs: Long): Flow<ConnectionType> = flow {
    var first = true
    emitAll(debounce { if (first) 0L.also { first = false } else settleMs })
}.distinctUntilChanged()
