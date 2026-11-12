package com.example.torrentstreamplayer.ui

import androidx.annotation.StringRes
import com.example.torrentstreamplayer.R
import com.example.torrentstreamplayer.engine.MagnetDisplayName
import com.example.torrentstreamplayer.engine.MagnetIdentity
import com.example.torrentstreamplayer.engine.TorrentInputResolver
import com.example.torrentstreamplayer.meta.NetworkSpec
import java.util.Locale

/** Kinds of link another app can hand over. [label] is the sheet's kind line ("마그넷 링크"). */
internal enum class IncomingLinkKind(@StringRes val label: Int) {
    /** A magnet link with a valid info hash ([MagnetIdentity]). */
    MAGNET(R.string.open_link_kind_magnet),

    /**
     * An https address, expected to serve a `.torrent` file. Its path may be anything (`download.php?id=3`):
     * [TorrentInputResolver] checks the downloaded bytes.
     */
    TORRENT_URL(R.string.open_link_kind_torrent),
}

/** A validated link from another app. [displayName] is the magnet's `dn`, if any; never log [value]. */
internal data class IncomingLink(val value: String, val kind: IncomingLinkKind, val displayName: String?) {
    /** The magnet key, or null for a `.torrent` address (known only after downloading the file). */
    val magnetKey: String? get() = if (kind == IncomingLinkKind.MAGNET) MagnetIdentity.key(value) else null
}

/**
 * Why a handed-over link cannot be opened. Each reason registers the error sheet's text
 * (`design/specs/open-link-sheet.md` 2-2, wording in `design/specs/error-recovery.md` 2-1).
 */
internal enum class IncomingLinkError(@StringRes val message: Int) {
    /** No `magnet:` or `http(s)://` link in the shared text, or another scheme. */
    UNSUPPORTED(R.string.error_unsupported_link),

    /** A `magnet:` link without a valid info hash. */
    INVALID_MAGNET(R.string.error_invalid_magnet),

    /** Longer than [NetworkSpec.MAX_LINK_CHARS], or cut off where the shared-text search ends. */
    TOO_LONG(R.string.error_link_length),

    /** A plain `http://` address. */
    HTTPS_REQUIRED(R.string.error_https_required),
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
 * (a later valid link is not tried, so the user sees why the first one fails). A link that runs past the searched
 * part is cut, so it is rejected ([IncomingLinkError.TOO_LONG]). A link may follow other letters directly
 * (`보세요magnet:?…`). A magnet ends only at whitespace, because its `dn` may hold unencoded Hangul; an https
 * address also ends at the first non-ASCII character. Closing punctuation such as `).` at its end is dropped.
 * The scheme is matched without case (`HTTPS://`, `Magnet:`) and handed on in lower case.
 */
internal object IncomingLinkParser {
    /** Longest part of shared text searched for a link; message apps can share whole conversations. */
    const val MAX_SHARED_CHARS = NetworkSpec.MAX_LINK_CHARS * 4
    private val LINK = Regex("""(?i)(?<![a-z0-9])(?:magnet:\?[^\s\p{Z}]+|https?://[\x21-\x7E]+)""")
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
        val searched = text.take(MAX_SHARED_CHARS)
        val found = LINK.find(searched) ?: return invalid(IncomingLinkError.UNSUPPORTED)
        val cut = found.range.last == searched.lastIndex && text.length > searched.length &&
            !text[searched.length].isWhitespace()
        if (cut) return invalid(IncomingLinkError.TOO_LONG)
        return link(found.value.trimEnd { it in TRAILING_PUNCTUATION })
    }

    /** [value] as one whole link: a magnet, or an https address with a host. */
    fun link(value: String): IncomingLinkResult {
        val trimmed = lowerCaseScheme(value.trim())
        return when {
            trimmed.isEmpty() -> invalid(IncomingLinkError.UNSUPPORTED)
            trimmed.length > NetworkSpec.MAX_LINK_CHARS -> invalid(IncomingLinkError.TOO_LONG)
            trimmed.startsWith(MAGNET_PREFIX) ->
                if (runCatching { MagnetIdentity.key(trimmed) }.isSuccess) {
                    val name = MagnetDisplayName.of(trimmed)
                    IncomingLinkResult.Valid(IncomingLink(trimmed, IncomingLinkKind.MAGNET, name))
                } else {
                    invalid(IncomingLinkError.INVALID_MAGNET)
                }
            trimmed.startsWith(HTTP_PREFIX) -> invalid(IncomingLinkError.HTTPS_REQUIRED)
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

    /** `HTTPS://Example.com/A` → `https://Example.com/A`: the resolver and the engine expect a lower-case scheme. */
    private fun lowerCaseScheme(value: String): String {
        val end = value.indexOf(':')
        return if (end <= 0) value else value.substring(0, end).lowercase(Locale.ROOT) + value.substring(end)
    }

    private fun invalid(reason: IncomingLinkError) = IncomingLinkResult.Invalid(reason)
}
