package com.example.torrentstreamplayer.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

class SubtitleSelectionPolicyTest {
    private fun sub(name: String, language: SubtitleLanguage?, matches: Boolean = true, fallback: Boolean = true) =
        SubtitleFile(File(name), language, matches, fallback)

    @Test fun noFilesMeansNoSubtitle() {
        assertNull(SubtitleSelectionPolicy.preferred(emptyList()))
    }

    @Test fun koreanBeatsEnglishBeatsUnmarked() {
        val unmarked = sub("a.srt", null)
        val english = sub("a.en.srt", SubtitleLanguage.ENGLISH)
        val korean = sub("a.ko.srt", SubtitleLanguage.KOREAN)
        assertEquals(korean, SubtitleSelectionPolicy.preferred(listOf(unmarked, english, korean)))
        assertEquals(english, SubtitleSelectionPolicy.preferred(listOf(unmarked, english)))
        assertEquals(unmarked, SubtitleSelectionPolicy.preferred(listOf(unmarked, sub("b.srt", null))))
    }

    @Test fun nameMatchesWinOverLanguageOnlyFiles() {
        val korean = sub("Subs/Korean.srt", SubtitleLanguage.KOREAN, matches = false)
        val sameName = sub("Movie.en.srt", SubtitleLanguage.ENGLISH)
        assertEquals(sameName, SubtitleSelectionPolicy.preferred(listOf(korean, sameName)))
    }

    @Test fun aLoneVideoMayUseALanguageOnlySubtitle() {
        val korean = sub("Subs/Korean.srt", SubtitleLanguage.KOREAN, matches = false)
        assertEquals(korean, SubtitleSelectionPolicy.preferred(listOf(korean)))
    }

    @Test fun aSeasonPackNeverFallsBackToAnotherEpisode() {
        val otherEpisode = sub("Show E02.ko.srt", SubtitleLanguage.KOREAN, matches = false, fallback = false)
        assertNull(SubtitleSelectionPolicy.preferred(listOf(otherEpisode)))
    }

    @Test fun fallbackIsOffUnlessTheEnginePlanAllowsIt() {
        val unplanned = SubtitleFile(File("Subs/Korean.srt"), SubtitleLanguage.KOREAN, matchesVideoName = false)
        assertNull(SubtitleSelectionPolicy.preferred(listOf(unplanned)))
    }
}
