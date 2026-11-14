package com.example.torrentstreamplayer.ui

import java.util.Locale
import kotlin.math.roundToInt

internal object PlayerOverlayPolicy {
    fun canSwipe(playerAvailable: Boolean, duration: Long, controls: Boolean): Boolean = controls && playerAvailable && duration > 0

    /** The seek bar takes touches only once the video length is known; before that a position means nothing. */
    fun seekEnabled(duration: Long): Boolean = duration > 0

    fun seekBarOnly(controls: Boolean, preview: Boolean): Boolean = !controls && preview

    /** The zoom chip on the top bar shows only when the zoom does not read "1.0×" (`design/specs/subtitles.md` 1절). */
    fun zoomChipVisible(scale: Float): Boolean = (scale * 10).roundToInt() != 10

    /** Zoom shown as "1.5×". */
    fun zoomLabel(scale: Float): String = String.format(Locale.US, "%.1f×", scale)
}
