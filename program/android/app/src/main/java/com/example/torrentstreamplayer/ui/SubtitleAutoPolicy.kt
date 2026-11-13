package com.example.torrentstreamplayer.ui

import com.example.torrentstreamplayer.engine.SubtitleFile
import com.example.torrentstreamplayer.engine.SubtitleLanguage
import com.example.torrentstreamplayer.engine.SubtitleSelectionPolicy

/**
 * An automatic subtitle pick. [final] is false while a file in the preferred language is still on its way: the pick
 * stands in until that file is ready, and then the preferred file replaces it (PR #142 review, PM decision).
 */
internal data class AutoSubtitleChoice(val trackId: Int, val final: Boolean)

/** What may stand in when nothing in the preferred language is ready (#170 PM decision). */
internal enum class SubtitleFallback {
    /**
     * The file [SubtitleSelectionPolicy] picks (Korean, English, then a lone file without a language), then an
     * embedded Korean track.
     */
    ANY,

    /** Only English: an English file, then an embedded English track. Nothing else is turned on by itself. */
    ENGLISH_ONLY,
}

/**
 * The language "show subtitles automatically" looks for. [keys] are the preference keys that name it
 * ([TrackLanguage.keyOf]): one key for a saved pick, the two- and three-letter codes for a device language
 * (`es`/`spa`), because VLC reports tracks by either code.
 */
internal data class SubtitlePreference(val keys: Set<String>, val fallback: SubtitleFallback) {
    fun matches(key: String?): Boolean = key != null && key in keys

    companion object {
        /** A saved pick ([key]), or the default (Korean) when there is none. Falls back as before. */
        fun saved(key: String?): SubtitlePreference =
            SubtitlePreference(setOf(key?.takeIf { it.isNotBlank() } ?: SubtitleAutoPolicy.DEFAULT_LANGUAGE_KEY),
                SubtitleFallback.ANY)
    }
}

/**
 * Which subtitle to turn on by itself when "show subtitles automatically" is on. The preferred language is the last
 * language the viewer picked (PM decision, PR #131); before any pick it is the device language ([preference], #161).
 *
 * Order: an external file in the preferred language (among [SubtitleSelectionPolicy.pool]), an embedded track in
 * that language, then the stand-in its [SubtitlePreference.fallback] allows. Null when none fits.
 */
internal object SubtitleAutoPolicy {
    /** Used only when neither a saved pick nor the device language gives a language key. */
    val DEFAULT_LANGUAGE_KEY: String = TrackLanguage.KOREAN.key

    /**
     * The language to prefer: the viewer's saved pick ([saved]) first, else the device language. [deviceLanguage] is
     * `Locale.language` ("ko", "en", "es"), [deviceIso3] is `Locale.getISO3Language()` ("kor", "eng", "spa").
     * A saved pick and a Korean device fall back as before ([SubtitleFallback.ANY]); any other device language falls
     * back to English only. Without any readable language it is [DEFAULT_LANGUAGE_KEY].
     */
    fun preference(saved: String?, deviceLanguage: String?, deviceIso3: String?): SubtitlePreference {
        if (!saved.isNullOrBlank()) return SubtitlePreference.saved(saved)
        val keys = TrackLanguage.keysOf(deviceLanguage, deviceIso3)
        if (keys.isEmpty() || TrackLanguage.KOREAN.key in keys) return SubtitlePreference.saved(null)
        return SubtitlePreference(keys, SubtitleFallback.ENGLISH_ONLY)
    }

    fun choose(
        preference: SubtitlePreference,
        embedded: List<PlayerTrack>,
        externals: List<ExternalSubtitle>,
    ): AutoSubtitleChoice? {
        val ready = externals.filter { it.state == ExternalSubtitleState.ADDED && it.trackId != null }
        val files = ready.mapNotNull { it.subtitle }
        fun trackOf(file: SubtitleFile?) = file?.let { chosen -> ready.firstOrNull { it.subtitle == chosen }?.trackId }
        val preferredFile = SubtitleSelectionPolicy.pool(files).firstOrNull { file ->
            preference.matches(file.language?.let(TrackLanguage::of)?.key)
        }
        trackOf(preferredFile)?.let { return AutoSubtitleChoice(it, final = true) }
        val preferredOnTheWay = externals.any {
            it.trackId == null && it.state != ExternalSubtitleState.FAILED && preference.matches(it.language?.key)
        }
        val preferredEmbedded = embedded.firstOrNull { preference.matches(SubtitleTrackLabel.languageKey(it)) }?.id
        // A stand-in already in the preferred language is kept: switching to the same language only flickers.
        if (preferredEmbedded != null) return AutoSubtitleChoice(preferredEmbedded, final = true)
        val standIn = standIn(preference.fallback, files, embedded, ::trackOf) ?: return null
        return AutoSubtitleChoice(standIn, final = !preferredOnTheWay)
    }

    private fun standIn(fallback: SubtitleFallback, files: List<SubtitleFile>, embedded: List<PlayerTrack>,
        trackOf: (SubtitleFile?) -> Int?): Int? = when (fallback) {
        SubtitleFallback.ANY -> trackOf(SubtitleSelectionPolicy.preferred(files))
            ?: embedded.firstOrNull { SubtitleTrackLabel.language(it) == TrackLanguage.KOREAN }?.id
        SubtitleFallback.ENGLISH_ONLY ->
            trackOf(SubtitleSelectionPolicy.pool(files).firstOrNull { it.language == SubtitleLanguage.ENGLISH })
                ?: embedded.firstOrNull { SubtitleTrackLabel.language(it) == TrackLanguage.ENGLISH }?.id
    }
}
