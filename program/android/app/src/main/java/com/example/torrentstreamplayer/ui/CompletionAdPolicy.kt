package com.example.torrentstreamplayer.ui

import com.example.torrentstreamplayer.meta.AdSpec
import com.example.torrentstreamplayer.meta.AdUnit

internal object CompletionAdPolicy {
    fun unitId(debug: Boolean): String = AdUnit.INTERSTITIAL.id(debug)
    fun nextCompletionCount(previous: Int): Int = previous.coerceIn(0, AdSpec.COMPLETION_COUNT - 1) + 1
    fun eligible(watchedMs: Long, nowMs: Long, lastShownMs: Long?, completions: Int = 0): Boolean =
        (watchedMs >= AdSpec.COMPLETION_MIN_WATCH_MS || completions >= AdSpec.COMPLETION_COUNT) &&
            (lastShownMs == null || nowMs - lastShownMs >= AdSpec.COMPLETION_COOLDOWN_MS)
}
