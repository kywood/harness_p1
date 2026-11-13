package com.example.torrentstreamplayer.ui

import com.example.torrentstreamplayer.engine.MagnetIdentity
import java.net.URI
import java.net.URLDecoder
import java.util.Locale

/**
 * The link sheet's summary card (`design/specs/open-link-sheet.md` 1-1). The full link is never shown: it is long and
 * an address's query may carry a token.
 *
 * @property name the name line; null for a magnet without a name, shown as [hashPrefix] instead.
 * @property hashPrefix first [IncomingLinkSummary.HASH_PREFIX_CHARS] info hash digits, upper case, when [name] is null.
 * @property showKind false when the name line already says "마그넷 링크".
 */
internal data class LinkSummary(
    val name: String?,
    val hashPrefix: String?,
    val kind: IncomingLinkKind,
    val showKind: Boolean = true,
)

/** Shortens an incoming link for the sheet. Pure; no Android or Compose. */
internal object IncomingLinkSummary {
    const val HASH_PREFIX_CHARS = 8
    private const val ELLIPSIS = "…"
    private val HASH_PREFIXES = listOf("urn:btih:", "urn:btmh:1220")

    /** @param playlistTitle the name saved in the playlist; preferred over `dn` because it is the received file's. */
    fun of(link: IncomingLink, playlistTitle: String? = null): LinkSummary = when (link.kind) {
        IncomingLinkKind.MAGNET -> {
            val name = listOf(playlistTitle, link.displayName).firstNotNullOfOrNull { it?.trim()?.ifEmpty { null } }
            if (name != null) LinkSummary(name, null, link.kind)
            else LinkSummary(null, hashPrefix(link.value), link.kind, showKind = false)
        }
        IncomingLinkKind.TORRENT_URL -> LinkSummary(shortAddress(link.value), null, link.kind)
    }

    /** First info hash digits of [magnet], or null when it has no readable `xt`. */
    fun hashPrefix(magnet: String): String? = magnet.substringAfter('?', "").split('&')
        .filter { it.substringBefore('=').equals("xt", ignoreCase = true) }
        .firstNotNullOfOrNull { part ->
            val topic = runCatching { URLDecoder.decode(part.substringAfter('='), "UTF-8") }.getOrNull()
            topic?.lowercase(Locale.ROOT)?.let(MagnetIdentity::normalize)
        }
        ?.let { topic -> HASH_PREFIXES.firstOrNull(topic::startsWith)?.let { topic.removePrefix(it) } }
        ?.take(HASH_PREFIX_CHARS)?.uppercase(Locale.ROOT)

    /** `https://example.org/a/b/file.torrent?t=1` → `example.org/…/file.torrent`; one path step keeps no `…/`. */
    fun shortAddress(address: String): String {
        val uri = runCatching { URI(address) }.getOrNull() ?: return address.substringBefore('?')
        val host = uri.host ?: return address.substringBefore('?')
        val steps = uri.path.orEmpty().split('/').filter { it.isNotEmpty() }
        return when (steps.size) {
            0 -> host
            1 -> "$host/${steps[0]}"
            else -> "$host/$ELLIPSIS/${steps.last()}"
        }
    }
}
