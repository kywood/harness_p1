package com.example.torrentstreamplayer.ui

import androidx.annotation.StringRes
import com.example.torrentstreamplayer.R
import com.example.torrentstreamplayer.engine.SubtitleLanguage
import java.util.Locale

/**
 * Languages the player names in Korean (`design/specs/subtitles.md` 2절). Register a language and the codes or
 * English names VLC reports for it here; track names and the remembered preference read this table.
 *
 * @param codes ISO 639-1/2 codes and the English name, lowercase. The first one is the [key] saved as preference.
 * @param subtitleLanguage the language the engine detects in subtitle file names, when it detects this one.
 */
internal enum class TrackLanguage(
    @StringRes val label: Int,
    private val codes: List<String>,
    val subtitleLanguage: SubtitleLanguage? = null,
) {
    KOREAN(R.string.lang_ko, listOf("ko", "kor", "korean"), SubtitleLanguage.KOREAN),
    ENGLISH(R.string.lang_en, listOf("en", "eng", "english"), SubtitleLanguage.ENGLISH),
    JAPANESE(R.string.lang_ja, listOf("ja", "jpn", "japanese")),
    CHINESE(R.string.lang_zh, listOf("zh", "chi", "zho", "chinese")),
    ;

    /** Saved preference value. */
    val key: String get() = codes.first()

    companion object {
        private val REGION = Regex("[-_].*$")
        private val CODE = Regex("[a-z]{2,8}")

        /** The language VLC names with [text] (`kor`, `ko-KR`, `Korean`), or null. */
        fun of(text: String?): TrackLanguage? {
            val code = normalize(text) ?: return null
            return entries.firstOrNull { code in it.codes }
        }

        fun of(language: SubtitleLanguage): TrackLanguage? = entries.firstOrNull { it.subtitleLanguage == language }

        /**
         * Preference key of the language in [text]: a registered language's [key], or the lowercase code itself
         * (`fre`) for other languages. Null when [text] is not a language code.
         */
        fun keyOf(text: String?): String? = of(text)?.key ?: normalize(text)

        /** ISO 639-2 codes that name no language (undetermined, multiple, none, uncoded). */
        private val NO_LANGUAGE = setOf("und", "mul", "zxx", "mis")

        private fun normalize(text: String?): String? =
            text?.trim()?.lowercase(Locale.ROOT)?.replace(REGION, "")
                ?.takeIf { CODE.matches(it) && it !in NO_LANGUAGE }
    }
}
