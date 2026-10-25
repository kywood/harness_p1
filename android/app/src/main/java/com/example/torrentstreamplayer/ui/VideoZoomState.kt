package com.example.torrentstreamplayer.ui

internal data class VideoZoomState(
    val scale: Float = 1f, val x: Float = 0f, val y: Float = 0f
) {
    fun transformed(factor: Float, panX: Float, panY: Float, width: Float, height: Float,
        focusX: Float = width / 2f, focusY: Float = height / 2f): VideoZoomState {
        if (!factor.isFinite() || factor <= 0 || !panX.isFinite() || !panY.isFinite() ||
            !width.isFinite() || !height.isFinite() || width <= 0 || height <= 0 ||
            !focusX.isFinite() || !focusY.isFinite()) return this
        val next = (scale * factor).coerceIn(1f, 4f)
        val ratio = next / scale
        val centerX = focusX - width / 2f
        val centerY = focusY - height / 2f
        val maxX = (next - 1f) * width / 2f
        val maxY = (next - 1f) * height / 2f
        return VideoZoomState(next,
            ((x - centerX) * ratio + centerX + panX).coerceIn(-maxX, maxX),
            ((y - centerY) * ratio + centerY + panY).coerceIn(-maxY, maxY))
    }
}
