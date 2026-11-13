package com.example.torrentstreamplayer.ui

/**
 * A subtitle or audio track as the player reports it.
 *
 * @param name VLC's track name, e.g. `Track 1 - [English]` or `SDH - [English]`.
 * @param languageCode the container's language code (`eng`), when the player gives one.
 * @param description the container's track title (`SDH`), when the player gives one.
 */
internal data class PlayerTrack(
    val id: Int,
    val name: String?,
    val languageCode: String? = null,
    val description: String? = null,
)

/** How a track is named on screen. The screen turns it into text with `strings.xml` (`TrackNameText`). */
internal sealed interface TrackName {
    /** "한국어". */
    data class Language(val language: TrackLanguage) : TrackName

    /** "영어 (SDH)": a language that appears more than once, told apart by its title. */
    data class LanguageDetail(val language: TrackLanguage, val detail: String) : TrackName

    /** "영어 2": a language that appears more than once with the same or no title. */
    data class LanguageNumber(val language: TrackLanguage, val number: Int) : TrackName

    /** The player's or the file's own name, shown as given. */
    data class Text(val text: String) : TrackName

    /** "트랙 N": a track with neither language nor title. [number] counts from 1 within its list. */
    data class Untitled(val number: Int) : TrackName

    /** An external subtitle file: its file name, and the language read from that name for the notice. */
    data class File(val fileName: String, val language: TrackLanguage?) : TrackName
}

/**
 * Names embedded tracks (`design/specs/subtitles.md` 2절 "목록 순서와 이름"): language in Korean, then the track's
 * title, then "트랙 N". VLC's English frame (`Track 1 - [English]`) is dropped when the language can be read from it.
 */
internal object SubtitleTrackLabel {
    /** VLC's `<title> - [<language>]`. */
    private val VLC_NAME = Regex("^(.*?)\\s+-\\s+\\[([^\\]]+)]$")

    /** VLC's placeholder titles that say nothing about the track. */
    private val GENERIC_TITLE = Regex("^(track|subtitles?|audio|stream)(\\s*\\d+)?$", RegexOption.IGNORE_CASE)

    fun names(tracks: List<PlayerTrack>): List<TrackName> {
        val parsed = tracks.map(::parse)
        val repeated = parsed.mapNotNull { it.language }.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        val plainCount = mutableMapOf<TrackLanguage, Int>()
        return parsed.mapIndexed { index, track ->
            val language = track.language
            when {
                language == null -> track.text?.let { TrackName.Text(it) } ?: TrackName.Untitled(index + 1)
                language !in repeated -> TrackName.Language(language)
                track.detail != null && parsed.count { it.language == language && it.detail == track.detail } == 1 ->
                    TrackName.LanguageDetail(language, track.detail)
                else -> {
                    val number = plainCount.merge(language, 1, Int::plus) ?: 1
                    if (number == 1) TrackName.Language(language) else TrackName.LanguageNumber(language, number)
                }
            }
        }
    }

    /** The language of [track], from its code first and then from VLC's name. */
    fun language(track: PlayerTrack): TrackLanguage? = parse(track).language

    /** Preference key of [track]'s language ([TrackLanguage.keyOf]), or null when it has none. */
    fun languageKey(track: PlayerTrack): String? =
        TrackLanguage.keyOf(track.languageCode) ?: language(track)?.key

    private data class Parsed(val language: TrackLanguage?, val detail: String?, val text: String?)

    private fun parse(track: PlayerTrack): Parsed {
        val name = track.name?.trim()?.takeIf { it.isNotEmpty() }
        val match = name?.let(VLC_NAME::find)
        val title = match?.groupValues?.get(1) ?: name
        val language = TrackLanguage.of(track.languageCode) ?: TrackLanguage.of(match?.groupValues?.get(2))
            ?: TrackLanguage.of(name)
        val detail = listOf(track.description, title).firstNotNullOfOrNull { informative(it) }
        val text = name?.takeUnless { GENERIC_TITLE.matches(it) } ?: informative(track.description)
        return Parsed(language, detail, text)
    }

    /** [text] when it tells something beyond a placeholder or the language name itself. */
    private fun informative(text: String?): String? = text?.trim()
        ?.takeIf { it.isNotEmpty() && !GENERIC_TITLE.matches(it) && TrackLanguage.of(it) == null }
}
