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
 * followed by a letter is a valid CP949 pair, but not one of those syllables. A lead byte cut off at the very end
 * of the file is allowed (a truncated download or copy). Pure: no files.
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

    /**
     * [bytes] decoded as CP949, or null when this device has no CP949 decoder. Called once [detect] said CP949, so a
     * byte that does not decode (a broken pair, a cut-off last byte) becomes U+FFFD instead of failing the whole file.
     * When only `EUC-KR` is available, UHC syllables outside KS X 1001 become U+FFFD too.
     */
    fun decodeCp949(bytes: ByteArray): String? {
        val charset = CP949_NAMES.firstOrNull { runCatching { Charset.isSupported(it) }.getOrDefault(false) }
            ?.let(Charset::forName) ?: return null
        return decode(charset, bytes, CodingErrorAction.REPLACE)
    }

    private fun isUtf8(bytes: ByteArray): Boolean = decode(Charsets.UTF_8, bytes, CodingErrorAction.REPORT) != null

    private fun decode(charset: Charset, bytes: ByteArray, onError: CodingErrorAction): String? = runCatching {
        charset.newDecoder()
            .onMalformedInput(onError)
            .onUnmappableCharacter(onError)
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
            val trail = bytes.getOrNull(i + 1)?.toInt()?.and(0xFF) ?: break
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
