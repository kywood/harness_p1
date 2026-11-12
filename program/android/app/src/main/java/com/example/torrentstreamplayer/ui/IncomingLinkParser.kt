package com.example.torrentstreamplayer.ui

import com.example.torrentstreamplayer.engine.MagnetDisplayName
import com.example.torrentstreamplayer.engine.MagnetIdentity
import com.example.torrentstreamplayer.engine.TorrentInputResolver
import com.example.torrentstreamplayer.meta.NetworkSpec
import java.util.Locale

/** Kinds of link another app can hand over. Register a new kind with how to recognise it. */
internal enum class IncomingLinkKind(private val accepts: (String) -> Boolean) {
    /** A magnet link with a valid info hash ([MagnetIdentity]). */
    MAGNET({ value -> value.startsWith("magnet:", ignoreCase = true) && runCatching { MagnetIdentity.key(value) }.isSuccess }),

    /** An http(s) address of a `.torrent` file ([TorrentInputResolver.webAddress]). */
    TORRENT_URL({ value ->
        TorrentInputResolver.webAddress(value)?.rawPath?.lowercase(Locale.ROOT)?.endsWith(".torrent") == true
    }),
    ;

    companion object {
        fun of(value: String): IncomingLinkKind? = entries.firstOrNull { it.accepts(value) }
    }
}

/** A validated link from another app. [displayName] is the magnet's `dn`, if any; never log [value]. */
internal data class IncomingLink(val value: String, val kind: IncomingLinkKind, val displayName: String?) {
    /** The magnet key, or null for a `.torrent` address (known only after downloading the file). */
    val magnetKey: String? get() = if (kind == IncomingLinkKind.MAGNET) MagnetIdentity.key(value) else null
}

/** Intent actions PiecePlay accepts links from. The action strings are Android's `Intent.ACTION_*` values. */
internal enum class IncomingAction(val action: String) {
    /** A tapped `magnet:` link: the link is the intent data. */
    VIEW("android.intent.action.VIEW"),

    /** Shared text: the link is somewhere in `Intent.EXTRA_TEXT`. */
    SEND("android.intent.action.SEND"),
    ;

    companion object {
        fun of(action: String?): IncomingAction? = entries.firstOrNull { it.action == action }
    }
}

/**
 * Reads a magnet link or http(s) `.torrent` address out of an intent's parts. Pure: the caller passes the intent's
 * action, data string and shared text. Input longer than [NetworkSpec.MAX_LINK_CHARS] or without a valid link gives
 * null. From shared text only the first valid link is taken.
 */
internal object IncomingLinkParser {
    /** Longest shared text scanned for a link; message apps can share whole conversations. */
    private const val MAX_SHARED_CHARS = NetworkSpec.MAX_LINK_CHARS * 4
    private val LINK = Regex("""(?i)\b(?:magnet:\?|https?://)\S+""")
    private const val TRAILING_PUNCTUATION = ".,;:!?)]}>\"'"

    fun parse(action: String?, data: String?, sharedText: String?): IncomingLink? = when (IncomingAction.of(action)) {
        IncomingAction.VIEW -> data?.let(::link)
        IncomingAction.SEND -> sharedText?.let(::firstLink)
        null -> null
    }

    /** The first valid link in [text], skipping words that only look like links. */
    fun firstLink(text: String): IncomingLink? {
        if (text.length > MAX_SHARED_CHARS) return null
        return LINK.findAll(text).firstNotNullOfOrNull { link(it.value.trimEnd { c -> c in TRAILING_PUNCTUATION }) }
    }

    /** [value] as a link if it is one whole, valid link. */
    fun link(value: String): IncomingLink? {
        val trimmed = value.trim()
        if (trimmed.isEmpty() || trimmed.length > NetworkSpec.MAX_LINK_CHARS) return null
        val kind = IncomingLinkKind.of(trimmed) ?: return null
        val name = if (kind == IncomingLinkKind.MAGNET) MagnetDisplayName.of(trimmed) else null
        return IncomingLink(trimmed, kind, name)
    }
}
