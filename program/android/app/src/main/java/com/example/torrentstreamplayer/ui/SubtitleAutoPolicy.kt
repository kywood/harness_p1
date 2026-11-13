package com.example.torrentstreamplayer.ui

import com.example.torrentstreamplayer.engine.SubtitleFile
import com.example.torrentstreamplayer.engine.SubtitleSelectionPolicy

/**
 * Which subtitle to turn on by itself when "자막 자동 표시" is on. The preferred language is the last language the
 * viewer picked (PM decision, PR #131); before any pick it is Korean.
 *
 * Order: an external file in the preferred language (named like the video first, see [SubtitleSelectionPolicy]),
 * an embedded track in that language, the external file [SubtitleSelectionPolicy] picks (Korean, English, then a
 * lone file without a language), an embedded Korean track. Null when none fits.
 */
internal object SubtitleAutoPolicy {
    val DEFAULT_LANGUAGE_KEY: String = TrackLanguage.KOREAN.key

    fun choose(preferredKey: String?, embedded: List<PlayerTrack>, externals: List<ExternalSubtitle>): Int? {
        val key = preferredKey ?: DEFAULT_LANGUAGE_KEY
        val ready = externals.filter { it.state == ExternalSubtitleState.ADDED && it.trackId != null }
        val files = ready.mapNotNull { it.subtitle }
        val pool = files.filter { it.matchesVideoName }.ifEmpty { files.filter { it.fallbackAllowed } }
        fun trackOf(file: SubtitleFile?) = file?.let { chosen -> ready.firstOrNull { it.subtitle == chosen }?.trackId }
        val preferredFile = pool.firstOrNull { file -> file.language?.let(TrackLanguage::of)?.key == key }
        return trackOf(preferredFile)
            ?: embedded.firstOrNull { SubtitleTrackLabel.languageKey(it) == key }?.id
            ?: trackOf(SubtitleSelectionPolicy.preferred(files))
            ?: embedded.firstOrNull { SubtitleTrackLabel.language(it) == TrackLanguage.KOREAN }?.id
    }
}
