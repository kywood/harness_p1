package com.example.torrentstreamplayer.engine

/**
 * Decides whether `TorrentEngine.stop()` has anything to tear down.
 *
 * Teardowns of every engine share one thread ([EngineTeardown]). An engine that holds nothing must not queue
 * an empty teardown: it would wait behind another engine's slow teardown (closing the native session can
 * take seconds) and the next start would time out although it has nothing to wait for.
 */
internal object EngineStopPlan {
    /**
     * @param started a worker thread was started since the last stop; it may still acquire the session or file.
     * @param session the engine holds a lease on the shared native session.
     * @param torrent the engine holds a torrent handle in that session.
     * @param checkpoint a resume checkpoint is open and must be saved and closed.
     */
    fun needsTeardown(started: Boolean, session: Boolean, torrent: Boolean, checkpoint: Boolean): Boolean =
        started || session || torrent || checkpoint
}
