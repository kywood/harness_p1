package com.example.torrentstreamplayer.ui

/**
 * A clock that runs only while [running] is true: the video plays and is not buffering. The subtitle session measures
 * how long an added file waits for its track with it, so a stalled stream does not count. Thread-safe: the screen
 * sets it on the main thread and the session reads it on the engine command queue.
 *
 * @param now wall clock in milliseconds; tests pass a fake one.
 */
internal class PlaybackClock(private val now: () -> Long = System::currentTimeMillis) {
    private var runningSince: Long? = null
    private var finishedRunsMs = 0L

    @Synchronized fun setRunning(running: Boolean) {
        val since = runningSince
        if (running && since == null) runningSince = now()
        if (!running && since != null) {
            finishedRunsMs += now() - since
            runningSince = null
        }
    }

    /** Running time so far, in milliseconds. */
    @Synchronized fun elapsedMs(): Long = finishedRunsMs + (runningSince?.let { now() - it } ?: 0L)
}
