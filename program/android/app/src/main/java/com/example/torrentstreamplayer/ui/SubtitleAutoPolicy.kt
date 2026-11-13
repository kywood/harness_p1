package com.example.torrentstreamplayer.ui

import com.example.torrentstreamplayer.engine.SubtitleFile
import com.example.torrentstreamplayer.engine.SubtitleSelectionPolicy

/**
 * An automatic subtitle pick. [final] is false while a file in the preferred language is still on its way: the pick
 * stands in until that file is ready, and then the preferred file replaces it (PR #142 review, PM decision).
 */
internal data class AutoSubtitleChoice(val trackId: Int, val final: Boolean)

/**
 * Which subtitle to turn on by itself when "show subtitles automatically" is on. The preferred language is the last
 * language the viewer picked (PM decision, PR #131); before any pick it is the device language ([preferredKey], #161).
 *
 * Order: an external file in the preferred language (among [SubtitleSelectionPolicy.pool]), an embedded track in
 * that language, the external file [SubtitleSelectionPolicy] picks (Korean, English, then a lone file without a
 * language), an embedded Korean track. Null when none fits.
 */
internal object SubtitleAutoPolicy {
    /** Used only when neither a saved pick nor the device language gives a language key. */
    val DEFAULT_LANGUAGE_KEY: String = TrackLanguage.KOREAN.key

    /**
     * The language to prefer: the viewer's saved pick ([saved]) first, else the device language ([deviceLanguage],
     * e.g. `Locale.language` "ko", "en"), else [DEFAULT_LANGUAGE_KEY]. A Korean device prefers Korean as before.
     */
    fun preferredKey(saved: String?, deviceLanguage: String?): String =
        saved?.takeIf { it.isNotBlank() } ?: TrackLanguage.keyOf(deviceLanguage) ?: DEFAULT_LANGUAGE_KEY

    fun choose(
        preferredKey: String?,
        embedded: List<PlayerTrack>,
        externals: List<ExternalSubtitle>,
    ): AutoSubtitleChoice? {
        val key = preferredKey ?: DEFAULT_LANGUAGE_KEY
        val ready = externals.filter { it.state == ExternalSubtitleState.ADDED && it.trackId != null }
        val files = ready.mapNotNull { it.subtitle }
        fun trackOf(file: SubtitleFile?) = file?.let { chosen -> ready.firstOrNull { it.subtitle == chosen }?.trackId }
        val preferredFile = SubtitleSelectionPolicy.pool(files).firstOrNull { file ->
            file.language?.let(TrackLanguage::of)?.key == key
        }
        trackOf(preferredFile)?.let { return AutoSubtitleChoice(it, final = true) }
        val preferredOnTheWay = externals.any {
            it.trackId == null && it.state != ExternalSubtitleState.FAILED && it.language?.key == key
        }
        val preferredEmbedded = embedded.firstOrNull { SubtitleTrackLabel.languageKey(it) == key }?.id
        // A stand-in already in the preferred language is kept: switching to the same language only flickers.
        if (preferredEmbedded != null) return AutoSubtitleChoice(preferredEmbedded, final = true)
        val standIn = trackOf(SubtitleSelectionPolicy.preferred(files))
            ?: embedded.firstOrNull { SubtitleTrackLabel.language(it) == TrackLanguage.KOREAN }?.id
            ?: return null
        return AutoSubtitleChoice(standIn, final = !preferredOnTheWay)
    }
}
