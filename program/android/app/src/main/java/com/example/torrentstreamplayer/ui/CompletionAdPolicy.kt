package com.example.torrentstreamplayer.ui

internal object CompletionAdPolicy {
    fun unitId(debug: Boolean): String = if (debug)
        "ca-app-pub-3940256099942544/1033173712" else "ca-app-pub-2418522550325553/6999280238"
    fun nextCompletionCount(previous: Int): Int = previous.coerceIn(0, 4) + 1
    fun eligible(watchedMs: Long, nowMs: Long, lastShownMs: Long?, completions: Int = 0): Boolean =
        (watchedMs >= 300_000 || completions >= 5) &&
            (lastShownMs == null || nowMs - lastShownMs >= 600_000)
}
