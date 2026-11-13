package com.example.torrentstreamplayer.ui

import com.example.torrentstreamplayer.engine.SubtitleFile
import com.example.torrentstreamplayer.engine.SubtitleLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SubtitleMenuPlanTest {
    private fun added(name: String, id: Int?, language: SubtitleLanguage? = null, matches: Boolean = true,
        fallback: Boolean = false) =
        ExternalSubtitle(File(name), SubtitleFile(File(name), language, matches, fallback), ExternalSubtitleState.ADDED, id)

    @Test fun noTracksYetIsLoading() {
        assertTrue(SubtitleMenuPlan.build(null, emptyList()).loading)
    }

    @Test fun embeddedFirstThenFilesByNameAndAudioOnlyWhenThereAreTwo() {
        val tracks = PlayerTracks(
            subtitles = listOf(PlayerTrack(1, "Track 1 - [Korean]"), PlayerTrack(7, "Track 2"), PlayerTrack(8, "Track 3")),
            audio = listOf(PlayerTrack(3, "Track 1 - [Japanese]")),
            subtitleId = 8,
        )
        val sheet = SubtitleMenuPlan.build(tracks, listOf(added("b.srt", 8), added("a.srt", 7)))
        assertEquals(listOf(TrackName.Language(TrackLanguage.KOREAN), TrackName.File("a.srt", null),
            TrackName.File("b.srt", null)), sheet.subtitles.map { it.name })
        assertEquals(OptionTag.FILE, sheet.subtitles[1].tag)
        assertEquals(TrackName.File("b.srt", null), sheet.selectedName)
        assertTrue(sheet.audio.isEmpty())
    }

    @Test fun offSelectionHasNoName() {
        val sheet = SubtitleMenuPlan.build(PlayerTracks(listOf(PlayerTrack(1, "Track 1"))), emptyList())
        assertEquals(PlayerTracks.NO_TRACK, sheet.selectedSubtitle)
        assertNull(sheet.selectedName)
    }

    @Test fun autoPolicyOrder() {
        val embeddedKorean = PlayerTrack(1, "Track 1 - [Korean]")
        val embeddedEnglish = PlayerTrack(2, "Track 2 - [English]")
        val fileEnglish = added("m.en.srt", 10, SubtitleLanguage.ENGLISH)
        val fileKorean = added("m.ko.srt", 11, SubtitleLanguage.KOREAN)
        fun pick(key: String?, embedded: List<PlayerTrack>, externals: List<ExternalSubtitle>) =
            SubtitleAutoPolicy.choose(key, embedded, externals)?.trackId
        assertEquals("preferred-language file first", 11,
            pick(null, listOf(embeddedKorean), listOf(fileEnglish, fileKorean)))
        assertEquals("then a preferred-language embedded track", 1, pick(null, listOf(embeddedKorean), listOf(fileEnglish)))
        assertEquals("then the file policy", 10, pick(null, listOf(embeddedEnglish), listOf(fileEnglish)))
        assertEquals("then embedded Korean", 1, pick("ja", listOf(embeddedKorean), emptyList()))
        assertNull(pick(null, listOf(embeddedEnglish), emptyList()))
        val otherEpisode = added("e2.ko.srt", 12, SubtitleLanguage.KOREAN, matches = false)
        assertNull("a file the plan does not allow is skipped", pick(null, emptyList(), listOf(otherEpisode)))
        val koreanOnTheWay = ExternalSubtitle(File("m.ko.srt"), null, ExternalSubtitleState.DOWNLOADING)
        assertEquals("a stand-in while the preferred file downloads", AutoSubtitleChoice(10, final = false),
            SubtitleAutoPolicy.choose(null, emptyList(), listOf(fileEnglish, koreanOnTheWay)))
        assertEquals(AutoSubtitleChoice(10, final = true), SubtitleAutoPolicy.choose(null, emptyList(), listOf(fileEnglish)))
    }

    @Test fun aPreferredEmbeddedStandInIsFinal() {
        val koreanOnTheWay = ExternalSubtitle(File("m.ko.srt"), null, ExternalSubtitleState.DOWNLOADING)
        assertEquals(AutoSubtitleChoice(1, final = true),
            SubtitleAutoPolicy.choose(null, listOf(PlayerTrack(1, "Track 1 - [Korean]")), listOf(koreanOnTheWay)))
    }

    @Test fun aNewSubtitleNoticeReplacesTheOldOne() {
        val english = PlayerNotice.SubtitleOn(TrackName.Language(TrackLanguage.ENGLISH))
        val korean = PlayerNotice.SubtitleOn(TrackName.Language(TrackLanguage.KOREAN))
        val shown = PlayerNoticeQueue().add(english).add(korean)
        assertEquals(korean, shown.current)
        assertNull(shown.next().current)
        val waiting = PlayerNoticeQueue().add(PlayerNotice.MobileData).add(english).add(korean)
        assertEquals(korean, waiting.next().current)
        assertNull(waiting.next().next().current)
    }

    @Test fun noticeQueueShowsOneAtATimeWithoutRepeats() {
        val korean = PlayerNotice.SubtitleOn(TrackName.Language(TrackLanguage.KOREAN))
        val queue = PlayerNoticeQueue().add(korean).add(PlayerNotice.MobileData).add(PlayerNotice.MobileData)
        assertEquals(korean, queue.current)
        assertEquals(PlayerNotice.MobileData, queue.next().current)
        assertNull(queue.next().next().current)
    }

    @Test fun zoomChipHidesAtOnePointZero() {
        assertTrue(!PlayerOverlayPolicy.zoomChipVisible(1f))
        assertTrue(!PlayerOverlayPolicy.zoomChipVisible(1.04f))
        assertTrue(PlayerOverlayPolicy.zoomChipVisible(1.25f))
        assertEquals("1.5×", PlayerOverlayPolicy.zoomLabel(1.5f))
    }
}
