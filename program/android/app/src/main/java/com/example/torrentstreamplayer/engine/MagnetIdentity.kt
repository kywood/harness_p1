package com.example.torrentstreamplayer.engine

import java.net.URI
import java.net.URLDecoder
import java.security.MessageDigest
import java.util.Locale

internal object MagnetIdentity {
    private val HEX_BTIH = Regex("urn:btih:[0-9a-f]{40}")
    private val BTMH = Regex("urn:btmh:1220[0-9a-f]{64}")
    private val BASE32_BTIH = Regex("urn:btih:[a-z2-7]{32}")
    fun key(magnet: String): String {
        val uri = URI(magnet.trim())
        if (!uri.scheme.equals("magnet", true)) throw InvalidMagnetException()
        if (!uri.rawSchemeSpecificPart.startsWith("?")) throw InvalidMagnetException()
        val query = uri.rawQuery ?: uri.rawSchemeSpecificPart.removePrefix("?")
        val topics = query.split('&').mapNotNull {
            val parts = it.split('=', limit = 2)
            if (parts.size == 2 && parts[0].equals("xt", true))
                URLDecoder.decode(parts[1], "UTF-8").lowercase(Locale.ROOT) else null
        }
        val identity = topics.firstNotNullOfOrNull(::normalize)
            ?: throw InvalidMagnetException()
        return MessageDigest.getInstance("SHA-256").digest(identity.toByteArray())
            .joinToString("") { "%02x".format(it.toInt() and 255) }
    }
    private fun normalize(topic: String): String? {
        if (topic.matches(HEX_BTIH)) return topic
        if (topic.matches(BTMH)) return topic
        if (!topic.matches(BASE32_BTIH)) return null
        var bits = 0
        var value = 0
        val result = ArrayList<Byte>()
        topic.removePrefix("urn:btih:").forEach { char ->
            value = (value shl 5) or "abcdefghijklmnopqrstuvwxyz234567".indexOf(char)
            bits += 5
            if (bits >= 8) { bits -= 8; result.add((value ushr bits).toByte()) }
        }
        return "urn:btih:" + result.joinToString("") { "%02x".format(it.toInt() and 255) }
    }
}

/** A magnet link without a valid info hash. The UI shows its own localized text for this case. */
internal class InvalidMagnetException : IllegalArgumentException("Magnet link needs a valid info hash")
