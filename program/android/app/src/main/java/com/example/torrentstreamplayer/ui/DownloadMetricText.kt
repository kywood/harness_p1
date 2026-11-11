package com.example.torrentstreamplayer.ui
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
internal object DownloadMetricText {
    fun build(size: Long?, downloaded: Long?, speed: Long?, accent: Color, muted: Color,
        velocity: Color, format: (Long) -> String): AnnotatedString = buildAnnotatedString {
        val base = DownloadProgressLabel.text(size, downloaded, format)
        append(base)
        addStyle(SpanStyle(color = muted), 0, length)
        if (size != null && size > 0 && downloaded != null) {
            val style = SpanStyle(color = accent, fontWeight = FontWeight.SemiBold)
            addStyle(style, 0, base.indexOf(" / "))
            addStyle(style, base.lastIndexOf(" · ") + 3, length)
        }
        val suffix = DownloadProgressLabel.speed(speed, format)
        if (suffix.isNotEmpty()) {
            val start = length
            append(suffix)
            addStyle(SpanStyle(color = velocity, fontWeight = FontWeight.Medium), start + 3, length)
        }
    }
}
