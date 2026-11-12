package com.example.torrentstreamplayer.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

class SubtitleSelectionPolicyTest {
    private fun sub(name: String, language: SubtitleLanguage?, matches: Boolean = true) =
        SubtitleFile(File(name), language, matches)

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

    @Test fun filesNamedLikeTheVideoWinOverOtherEpisodes() {
        val otherEpisode = sub("E02.ko.srt", SubtitleLanguage.KOREAN, matches = false)
        val sameEpisode = sub("E01.en.srt", SubtitleLanguage.ENGLISH)
        assertEquals(sameEpisode, SubtitleSelectionPolicy.preferred(listOf(otherEpisode, sameEpisode)))
        assertEquals(otherEpisode, SubtitleSelectionPolicy.preferred(listOf(otherEpisode)))
    }
}
