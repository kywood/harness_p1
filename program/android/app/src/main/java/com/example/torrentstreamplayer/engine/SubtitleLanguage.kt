package com.example.torrentstreamplayer.engine

import java.util.Locale

/**
 * Subtitle languages guessed from a file name. Register a language and its markers here;
 * detection and the selection order ([SubtitleSelectionPolicy]) read this table.
 *
 * Latin markers must be a whole name token (`Movie.ko.srt`, `Movie_eng.srt`), so `Kong.srt` is not Korean.
 * Hangul markers may appear anywhere in the name (`영화 한글자막.srt`).
 * Entries are in selection order: earlier languages are preferred.
 */
internal enum class SubtitleLanguage(private val tokens: Set<String>, private val fragments: Set<String> = emptySet()) {
    KOREAN(setOf("ko", "kor", "korean"), setOf("한글", "한국어")),
    ENGLISH(setOf("en", "eng", "english")),
    ;

    private fun matches(nameTokens: Set<String>, name: String): Boolean =
        tokens.any { it in nameTokens } || fragments.any { it in name }

    companion object {
        private val SEPARATORS = Regex("[^\\p{L}\\p{N}]+")

        /** The language marked in [fileName] (with or without folders and extension), or null. */
        fun of(fileName: String): SubtitleLanguage? {
            val name = baseName(fileName).lowercase(Locale.ROOT)
            val nameTokens = name.split(SEPARATORS).filter { it.isNotEmpty() }.toSet()
            return entries.firstOrNull { it.matches(nameTokens, name) }
        }

        /** [path] without folders and without its last extension. */
        internal fun baseName(path: String): String =
            path.substringAfterLast('/').substringAfterLast('\\').substringBeforeLast('.')
    }
}
