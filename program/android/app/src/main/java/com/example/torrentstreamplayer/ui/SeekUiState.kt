package com.example.torrentstreamplayer.ui

internal data class SeekUiState(val targetMs: Long? = null, val buffering: Boolean = false, val fromMs: Long? = null) {
    val waiting: Boolean get() = targetMs != null || buffering
    val waitingMessage: String get() = if (targetMs != null) "이동한 구간을 준비하고 있습니다…" else "재생 데이터를 버퍼링하고 있습니다…"
    fun requested(target: Long, duration: Long, from: Long? = null): SeekUiState =
        copy(targetMs = target.coerceIn(0, duration.coerceAtLeast(0)), buffering = false, fromMs = from)
    fun timeChanged(position: Long): SeekUiState =
        if (targetMs != null && (kotlin.math.abs(position - targetMs) <= 1500 ||
                fromMs != null && if (targetMs >= fromMs) position >= targetMs else position <= targetMs))
            copy(targetMs = null) else this
    fun buffered(percent: Float): SeekUiState = copy(buffering = percent < 100f)
}
