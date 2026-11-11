package com.example.torrentstreamplayer.ui

internal class PlaybackExitGate {
    private var handled = false
    fun consume(): Boolean {
        if (handled) return false
        handled = true
        return true
    }
}
