package com.example.torrentstreamplayer.meta

/** Engine timing values. Register new engine timings here instead of writing literals at call sites. */
internal object EngineSpec {
    /** How long background teardown waits for libtorrent to deliver resume data. */
    const val RESUME_SAVE_WAIT_MS = 1_500L

    /** How long a caller waits for earlier teardown before restarting or deleting files. */
    const val TEARDOWN_AWAIT_MS = 10_000L
}
