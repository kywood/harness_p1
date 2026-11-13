package com.example.torrentstreamplayer.ui

import com.example.torrentstreamplayer.engine.SubtitleLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SubtitleTrackLabelTest {
    private fun names(vararg tracks: PlayerTrack) = SubtitleTrackLabel.names(tracks.toList())

    @Test fun languageCodesAndEnglishNamesMapToKoreanNames() {
        assertEquals(TrackLanguage.KOREAN, TrackLanguage.of("kor"))
        assertEquals(TrackLanguage.KOREAN, TrackLanguage.of("ko-KR"))
        assertEquals(TrackLanguage.ENGLISH, TrackLanguage.of("English"))
        assertEquals(TrackLanguage.JAPANESE, TrackLanguage.of("jpn"))
        assertEquals(TrackLanguage.CHINESE, TrackLanguage.of("zho"))
        assertNull(TrackLanguage.of("fre"))
        assertNull(TrackLanguage.of("Track 1"))
        assertEquals(TrackLanguage.ENGLISH, TrackLanguage.of(SubtitleLanguage.ENGLISH))
    }

    @Test fun preferenceKeysKeepUnknownCodesButNotUndetermined() {
        assertEquals("ko", TrackLanguage.keyOf("kor"))
        assertEquals("fre", TrackLanguage.keyOf("FRE"))
        assertNull(TrackLanguage.keyOf("und"))
        assertNull(TrackLanguage.keyOf("Track 1"))
    }

    @Test fun vlcEnglishFrameIsDroppedWhenTheLanguageIsKnown() {
        assertEquals(listOf(TrackName.Language(TrackLanguage.ENGLISH), TrackName.Language(TrackLanguage.KOREAN)),
            names(PlayerTrack(1, "Track 1 - [English]"), PlayerTrack(2, "Korean - [Korean]")))
    }

    @Test fun unknownLanguagesKeepTheOriginalNameAndEmptyTracksAreNumbered() {
        assertEquals(listOf(TrackName.Text("Track 1 - [French]"), TrackName.Untitled(2), TrackName.Untitled(3),
            TrackName.Text("Commentary")),
            names(PlayerTrack(1, "Track 1 - [French]"), PlayerTrack(2, null), PlayerTrack(3, "Track 3"),
                PlayerTrack(4, "Track 4", description = "Commentary")))
    }

    @Test fun repeatedLanguagesAreToldApartByTitleThenNumber() {
        assertEquals(
            listOf(TrackName.LanguageDetail(TrackLanguage.ENGLISH, "SDH"), TrackName.Language(TrackLanguage.ENGLISH),
                TrackName.LanguageNumber(TrackLanguage.ENGLISH, 2), TrackName.Language(TrackLanguage.KOREAN)),
            names(PlayerTrack(1, "SDH - [English]"), PlayerTrack(2, "Track 2 - [English]"),
                PlayerTrack(3, "Track 3", languageCode = "eng"), PlayerTrack(4, "Track 4 - [Korean]")))
    }

    @Test fun languageKeyPrefersTheContainerCode() {
        assertEquals("ja", SubtitleTrackLabel.languageKey(PlayerTrack(1, "Track 1 - [English]", languageCode = "jpn")))
        assertEquals("en", SubtitleTrackLabel.languageKey(PlayerTrack(1, "Track 1 - [English]")))
        assertNull(SubtitleTrackLabel.languageKey(PlayerTrack(1, "Track 1")))
    }
}
