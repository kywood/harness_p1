package com.example.torrentstreamplayer.ui

import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

/** How a subtitle file's bytes are encoded, as far as the player needs to know. */
internal enum class SubtitleEncoding {
    /** UTF-8, with or without BOM, or plain ASCII: the player reads it as it is. */
    UTF8,

    /** Starts with a UTF-16 BOM: the player reads it as it is. */
    UTF16,

    /** Korean CP949 (EUC-KR): re-encoded to UTF-8 before the player gets it. */
    CP949,

    /** Anything else (CP1252 English, …): left to the player's default. */
    OTHER,
}

/**
 * Decides from a subtitle file's own bytes whether it is Korean CP949, so a Korean `.smi` shows correctly whatever the
 * device language is, while a CP1252 English file (curly quotes `0x92`) is not read as Korean (PR #142 review).
 *
 * CP949 means: not valid UTF-8, every non-ASCII byte forms a CP949 pair, and at least half of the pairs are Hangul
 * syllables of KS X 1001 (lead `0xB0..0xC8`, trail `0xA1..0xFE`), which Korean text is made of. A CP1252 quote
 * followed by a letter is a valid CP949 pair, but not one of those syllables. Pure: no files.
 */
internal object SubtitleEncodingPolicy {
    private val UTF8_BOM = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
    private val CP949_NAMES = listOf("x-windows-949", "windows-949", "MS949", "EUC-KR")

    fun detect(bytes: ByteArray): SubtitleEncoding = when {
        bytes.startsWith(UTF8_BOM) || isUtf8(bytes) -> SubtitleEncoding.UTF8
        bytes.size >= 2 && (bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte() ||
            bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) -> SubtitleEncoding.UTF16
        looksKorean(bytes) -> SubtitleEncoding.CP949
        else -> SubtitleEncoding.OTHER
    }

    /** [bytes] decoded as CP949, or null when this device has no CP949 decoder or a byte does not decode. */
    fun decodeCp949(bytes: ByteArray): String? {
        val charset = CP949_NAMES.firstOrNull { runCatching { Charset.isSupported(it) }.getOrDefault(false) }
            ?.let(Charset::forName) ?: return null
        return strictDecode(charset, bytes)
    }

    private fun isUtf8(bytes: ByteArray): Boolean = strictDecode(Charsets.UTF_8, bytes) != null

    private fun strictDecode(charset: Charset, bytes: ByteArray): String? = runCatching {
        charset.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes)).toString()
    }.getOrNull()

    private fun looksKorean(bytes: ByteArray): Boolean {
        var pairs = 0
        var hangul = 0
        var i = 0
        while (i < bytes.size) {
            val lead = bytes[i].toInt() and 0xFF
            if (lead < 0x80) {
                i++
                continue
            }
            val trail = bytes.getOrNull(i + 1)?.toInt()?.and(0xFF) ?: return false
            if (lead !in 0x81..0xFE || !isCp949Trail(trail)) return false
            pairs++
            if (lead in 0xB0..0xC8 && trail in 0xA1..0xFE) hangul++
            i += 2
        }
        return hangul > 0 && hangul * 2 >= pairs
    }

    private fun isCp949Trail(byte: Int) = byte in 0x41..0x5A || byte in 0x61..0x7A || byte in 0x81..0xFE

    private fun ByteArray.startsWith(prefix: ByteArray) =
        size >= prefix.size && prefix.indices.all { this[it] == prefix[it] }
}
