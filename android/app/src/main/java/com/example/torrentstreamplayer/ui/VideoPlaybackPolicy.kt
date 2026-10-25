package com.example.torrentstreamplayer.ui

internal object VideoPlaybackPolicy {
    val options = arrayListOf("--drop-late-frames", "--skip-frames", "--file-caching=2000", "-vvv")
    fun hardwareDecoding(hardware: String): Boolean =
        !hardware.contains("goldfish", ignoreCase = true) && !hardware.contains("ranchu", ignoreCase = true)
}
