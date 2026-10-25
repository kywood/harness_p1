package com.example.torrentstreamplayer.engine

import java.net.URI
import java.net.URLDecoder
import java.security.MessageDigest
import java.util.Locale

internal object MagnetIdentity {
    fun key(magnet: String): String {
        val uri = URI(magnet.trim())
        require(uri.scheme.equals("magnet", true)) { "Invalid magnet scheme" }
        require(uri.rawSchemeSpecificPart.startsWith("?")) { "Invalid magnet query" }
        val query = uri.rawQuery ?: uri.rawSchemeSpecificPart.removePrefix("?")
        val topics = query.split('&').mapNotNull {
            val parts = it.split('=', limit = 2)
            if (parts.size == 2 && parts[0].equals("xt", true))
                URLDecoder.decode(parts[1], "UTF-8").lowercase(Locale.ROOT) else null
        }
        val identity = topics.firstNotNullOfOrNull(::normalize)
            ?: throw IllegalArgumentException("Magnet link needs a valid info hash")
        return MessageDigest.getInstance("SHA-256").digest(identity.toByteArray())
            .joinToString("") { "%02x".format(it.toInt() and 255) }
    }
    private fun normalize(topic: String): String? {
        if (topic.matches(Regex("urn:btih:[0-9a-f]{40}"))) return topic
        if (topic.matches(Regex("urn:btmh:1220[0-9a-f]{64}"))) return topic
        if (!topic.matches(Regex("urn:btih:[a-z2-7]{32}"))) return null
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
