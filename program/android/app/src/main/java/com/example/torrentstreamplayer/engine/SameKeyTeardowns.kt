package com.example.torrentstreamplayer.engine

import com.example.torrentstreamplayer.meta.EngineSpec
import java.util.concurrent.ConcurrentHashMap

/**
 * Remembers, per magnet key, the teardown of the engine that last stopped that torrent.
 *
 * A stopped engine keeps saving resume data and holds the torrent in the shared native session until its
 * teardown ends. Another engine that starts the same key must wait for that; an engine that starts a
 * different key has nothing to wait for.
 */
internal class SameKeyTeardowns {
    private val latest = ConcurrentHashMap<String, EngineTeardown>()

    /** Call after the engine that ran [key] has submitted its [teardown]. */
    fun stopped(key: String, teardown: EngineTeardown) {
        latest[key] = teardown
    }

    /** Returns true when no engine is still tearing down [key] or it finished within [timeoutMs]. */
    fun await(key: String, timeoutMs: Long = EngineSpec.TEARDOWN_AWAIT_MS): Boolean {
        val teardown = latest[key] ?: return true
        if (!teardown.await(timeoutMs)) return false
        latest.remove(key, teardown)
        return true
    }
}
