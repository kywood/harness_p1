package com.example.torrentstreamplayer.ui

internal object CompletionAdPolicy {
    fun unitId(debug: Boolean): String = if (debug)
        "ca-app-pub-3940256099942544/1033173712" else "ca-app-pub-2418522550325553/6999280238"
    fun eligible(watchedMs: Long, nowMs: Long, lastShownMs: Long?): Boolean =
        watchedMs >= 300_000 && (lastShownMs == null || nowMs - lastShownMs >= 600_000)
}
