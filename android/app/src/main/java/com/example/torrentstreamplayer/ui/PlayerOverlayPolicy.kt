package com.example.torrentstreamplayer.ui
internal object PlayerOverlayPolicy {
    fun canSwipe(playerAvailable: Boolean, duration: Long): Boolean = playerAvailable && duration > 0
    fun seekBarOnly(controls: Boolean, preview: Boolean): Boolean = !controls && preview
}
