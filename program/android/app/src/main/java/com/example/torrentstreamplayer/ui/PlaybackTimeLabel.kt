package com.example.torrentstreamplayer.ui

import java.util.Locale

/** Download speed on the player stats chip: "1.20 MB/s" or "512 KB/s". */
internal fun formatSpeed(bytesPerSec: Long): String {
    val kb = bytesPerSec / 1024
    val mb = kb / 1024.0
    return if (mb >= 1.0) {
        String.format(Locale.US, "%.2f MB/s", mb)
    } else {
        "$kb KB/s"
    }
}

/** Playback time: "05:07" or "1:02:03". */
internal fun formatTime(timeMs: Long): String {
    if (timeMs <= 0) return "00:00"
    val totalSeconds = timeMs / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}
