package com.example.torrentstreamplayer.ui

internal object DownloadProgressLabel {
    fun speed(bytesPerSecond: Long?, format: (Long) -> String): String =
        bytesPerSecond?.let { " · ${format(it.coerceAtLeast(0))}/s" }.orEmpty()
    fun text(size: Long?, downloaded: Long?, format: (Long) -> String): String {
        val total = size?.takeIf { it > 0 }
        val done = total?.let { downloaded?.coerceIn(0, it) }
        val percent = if (done != null) String.format(java.util.Locale.getDefault(), "%.1f%%", done * 100.0 / total!!) else "확인 전"
        return "${done?.let(format) ?: "—"} / ${total?.let(format) ?: "—"} · $percent"
    }
}
