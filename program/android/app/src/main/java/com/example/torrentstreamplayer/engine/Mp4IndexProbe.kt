package com.example.torrentstreamplayer.engine

internal object Mp4IndexProbe {
    sealed interface Result
    data class Need(val offset: Long, val length: Int) : Result
    data class Found(val offset: Long, val length: Long) : Result
    data object Unknown : Result
    fun inspect(size: Long, read: (Long, Int) -> ByteArray?): Result {
        var position = 0L
        repeat(256) {
            if (position < 0 || size - position < 8) return Unknown
            val header = read(position, 8) ?: return Need(position, 8)
            if (header.size != 8) return Unknown
            val buffer = java.nio.ByteBuffer.wrap(header)
            val size32 = buffer.int.toLong() and 0xffffffffL
            val type = String(header, 4, 4, Charsets.ISO_8859_1)
            var headerSize = 8L
            val length = when (size32) {
                0L -> size - position
                1L -> {
                    if (size - position < 16) return Unknown
                    val extended = read(position + 8, 8) ?: return Need(position + 8, 8)
                    if (extended.size != 8) return Unknown
                    headerSize = 16
                    java.nio.ByteBuffer.wrap(extended).long
                }
                else -> size32
            }
            if (length < headerSize || length > size - position) return Unknown
            if (type == "moov") return Found(position, length)
            position += length
        }
        return Unknown
    }
}
