package com.example.torrentstreamplayer.engine

import java.net.URI
import java.net.URLDecoder

/** The name a magnet link carries in its `dn` parameter, for showing a link before its metadata arrives. */
internal object MagnetDisplayName {
    /** Longest name kept from a link. */
    const val MAX_CHARS = 160

    /** Decoded `dn` of [magnet], cut to [MAX_CHARS]; null when missing, blank or unreadable. */
    fun of(magnet: String): String? = runCatching {
        URI(magnet.trim()).rawSchemeSpecificPart.removePrefix("?").split('&')
            .firstOrNull { it.startsWith("dn=") }?.substringAfter('=')
            ?.let { URLDecoder.decode(it, "UTF-8") }?.take(MAX_CHARS)?.takeIf { it.isNotBlank() }
    }.getOrNull()
}
