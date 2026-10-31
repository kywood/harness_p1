package com.example.torrentstreamplayer.engine
internal class NativeSessionLease<T : Any>(private val close: (T) -> Unit) {
    private var current: T? = null
    private val owners = java.util.IdentityHashMap<Any, Boolean>()
    @Synchronized fun acquire(owner: Any, create: () -> T): T {
        val shared = current ?: create().also { current = it }
        owners[owner] = true
        return shared
    }
    @Synchronized fun release(owner: Any) {
        if (owners.remove(owner) == null || owners.isNotEmpty()) return
        val shared = current ?: return
        try { close(shared); current = null }
        catch (error: Throwable) { owners[owner] = true; throw error }
    }
    @Synchronized fun ownerCount(): Int = owners.size
}
