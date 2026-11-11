package com.example.torrentstreamplayer.ui
internal object PlayerOverlayPolicy {
    fun canSwipe(playerAvailable: Boolean, duration: Long, controls: Boolean): Boolean = controls && playerAvailable && duration > 0
    fun seekBarOnly(controls: Boolean, preview: Boolean): Boolean = !controls && preview
}
