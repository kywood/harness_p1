package com.example.torrentstreamplayer.engine

internal object MetadataWait {
    fun <T : Any> await(timeoutMillis: Long, now: () -> Long = { System.nanoTime() / 1_000_000 },
        pause: () -> Unit = { Thread.sleep(250) }, poll: (Long) -> T?): T? {
        require(timeoutMillis > 0)
        val started = now()
        while (true) {
            if (Thread.currentThread().isInterrupted) throw InterruptedException()
            val elapsed = (now() - started).coerceAtLeast(0)
            poll(elapsed)?.let { return it }
            if (elapsed >= timeoutMillis) return null
            pause()
        }
    }
}
