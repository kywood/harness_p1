package com.example.torrentstreamplayer.ui

import androidx.annotation.StringRes
import com.example.torrentstreamplayer.R

internal data class SeekUiState(val targetMs: Long? = null, val buffering: Boolean = false, val fromMs: Long? = null) {
    val waiting: Boolean get() = targetMs != null || buffering
    @get:StringRes val waitingMessage: Int
        get() = if (targetMs != null) R.string.player_waiting_seek else R.string.player_waiting_buffering
    fun requested(target: Long, duration: Long, from: Long? = null): SeekUiState =
        copy(targetMs = target.coerceIn(0, duration.coerceAtLeast(0)), buffering = false, fromMs = from)
    fun timeChanged(position: Long): SeekUiState =
        if (targetMs != null && (kotlin.math.abs(position - targetMs) <= 1500 ||
                fromMs != null && if (targetMs >= fromMs) position >= targetMs else position <= targetMs))
            copy(targetMs = null) else this
    fun buffered(percent: Float): SeekUiState = copy(buffering = percent < 100f)
}
