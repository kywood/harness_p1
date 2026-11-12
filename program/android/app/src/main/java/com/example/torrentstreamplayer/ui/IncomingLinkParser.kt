package com.example.torrentstreamplayer.ui

import com.example.torrentstreamplayer.engine.MagnetDisplayName
import com.example.torrentstreamplayer.engine.MagnetIdentity
import com.example.torrentstreamplayer.engine.TorrentInputResolver
import com.example.torrentstreamplayer.meta.NetworkSpec
import java.util.Locale

/** Kinds of link another app can hand over. */
internal enum class IncomingLinkKind {
    /** A magnet link with a valid info hash ([MagnetIdentity]). */
    MAGNET,

    /**
     * An https address, expected to serve a `.torrent` file. Its path may be anything (`download.php?id=3`):
     * [TorrentInputResolver] checks the downloaded bytes.
     */
    TORRENT_URL,
}

/** A validated link from another app. [displayName] is the magnet's `dn`, if any; never log [value]. */
internal data class IncomingLink(val value: String, val kind: IncomingLinkKind, val displayName: String?) {
    /** The magnet key, or null for a `.torrent` address (known only after downloading the file). */
    val magnetKey: String? get() = if (kind == IncomingLinkKind.MAGNET) MagnetIdentity.key(value) else null
}

/**
 * Why a handed-over link cannot be opened. Each reason matches one error text of the design's error sheet
 * (`design/specs/open-link-sheet.md` 2-2); the strings are added with the sheet.
 */
internal enum class IncomingLinkError {
    /** No `magnet:` or `http(s)://` link in the shared text, or another scheme (`error_unsupported_link`). */
    UNSUPPORTED,

    /** A `magnet:` link without a valid info hash (`error_invalid_magnet`). */
    INVALID_MAGNET,

    /** Longer than [NetworkSpec.MAX_LINK_CHARS] (`error_link_length`). */
    TOO_LONG,

    /** A plain `http://` address (`error_https_required`). */
    HTTPS_REQUIRED,
}

/** Result of reading an intent: a link to confirm, or the reason it cannot be opened. */
internal sealed interface IncomingLinkResult {
    data class Valid(val link: IncomingLink) : IncomingLinkResult
    data class Invalid(val reason: IncomingLinkError) : IncomingLinkResult
}

/** Intent actions PiecePlay accepts links from. The action strings are Android's `Intent.ACTION_*` values. */
internal enum class IncomingAction(val action: String) {
    /** A tapped `magnet:` link: the link is the intent data. Only magnet links are accepted this way. */
    VIEW("android.intent.action.VIEW"),

    /** Shared text: the first link somewhere in `Intent.EXTRA_TEXT`. Magnet or http(s). */
    SEND("android.intent.action.SEND"),
    ;

    companion object {
        fun of(action: String?): IncomingAction? = entries.firstOrNull { it.action == action }
    }
}

/**
 * Reads a magnet link or https `.torrent` address out of an intent's parts. Pure: the caller passes the intent's
 * action, data string and shared text.
 *
 * Shared text: only its first [MAX_SHARED_CHARS] characters are searched, and only the first link found is judged
 * (a later valid link is not tried, so the user sees why the first one fails). A link may follow other letters
 * directly (`보세요magnet:?…`) and ends at whitespace or the first non-ASCII character; closing punctuation such as
 * `).` at its end is dropped.
 */
internal object IncomingLinkParser {
    /** Longest part of shared text searched for a link; message apps can share whole conversations. */
    const val MAX_SHARED_CHARS = NetworkSpec.MAX_LINK_CHARS * 4
    private val LINK = Regex("""(?i)(?<![a-z0-9])(?:magnet:\?|https?://)[\x21-\x7E]+""")
    private const val TRAILING_PUNCTUATION = ".,;:!?)]}>\"'"
    private const val MAGNET_PREFIX = "magnet:"
    private const val HTTP_PREFIX = "http://"

    /** Null when the intent is not a link hand-over (for example the launcher's MAIN intent). */
    fun parse(action: String?, data: String?, sharedText: String?): IncomingLinkResult? =
        when (IncomingAction.of(action)) {
            IncomingAction.VIEW -> viewLink(data)
            IncomingAction.SEND -> firstLink(sharedText.orEmpty())
            null -> null
        }

    /** The first link in [text], judged on its own. */
    fun firstLink(text: String): IncomingLinkResult {
        val found = LINK.find(text.take(MAX_SHARED_CHARS))
            ?: return IncomingLinkResult.Invalid(IncomingLinkError.UNSUPPORTED)
        return link(found.value.trimEnd { it in TRAILING_PUNCTUATION })
    }

    /** [value] as one whole link: a magnet, or an https address with a host. */
    fun link(value: String): IncomingLinkResult {
        val trimmed = value.trim()
        val lower = trimmed.lowercase(Locale.ROOT)
        return when {
            trimmed.isEmpty() -> invalid(IncomingLinkError.UNSUPPORTED)
            trimmed.length > NetworkSpec.MAX_LINK_CHARS -> invalid(IncomingLinkError.TOO_LONG)
            lower.startsWith(MAGNET_PREFIX) ->
                if (runCatching { MagnetIdentity.key(trimmed) }.isSuccess) {
                    val name = MagnetDisplayName.of(trimmed)
                    IncomingLinkResult.Valid(IncomingLink(trimmed, IncomingLinkKind.MAGNET, name))
                } else {
                    invalid(IncomingLinkError.INVALID_MAGNET)
                }
            lower.startsWith(HTTP_PREFIX) -> invalid(IncomingLinkError.HTTPS_REQUIRED)
            TorrentInputResolver.webAddress(trimmed) != null ->
                IncomingLinkResult.Valid(IncomingLink(trimmed, IncomingLinkKind.TORRENT_URL, null))
            else -> invalid(IncomingLinkError.UNSUPPORTED)
        }
    }

    /** A tapped link: only `magnet:` is accepted, even though the activity is exported to explicit intents. */
    private fun viewLink(data: String?): IncomingLinkResult {
        val value = data?.trim().orEmpty()
        if (!value.lowercase(Locale.ROOT).startsWith(MAGNET_PREFIX)) return invalid(IncomingLinkError.UNSUPPORTED)
        return link(value)
    }

    private fun invalid(reason: IncomingLinkError) = IncomingLinkResult.Invalid(reason)
}
