package com.example.torrentstreamplayer.ui

import com.example.torrentstreamplayer.engine.SubtitleFile
import com.example.torrentstreamplayer.engine.SubtitleLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

class SubtitleAutoPolicyTest {
    private fun file(name: String, id: Int, language: SubtitleLanguage?) =
        ExternalSubtitle(File(name), SubtitleFile(File(name), language, true, false), ExternalSubtitleState.ADDED, id)

    private val koreanFile = file("m.ko.srt", 11, SubtitleLanguage.KOREAN)
    private val englishFile = file("m.en.srt", 10, SubtitleLanguage.ENGLISH)
    private val loneFile = file("m.srt", 12, null)
    private val embeddedKorean = PlayerTrack(1, "Track 1 - [Korean]")
    private val embeddedEnglish = PlayerTrack(2, "Track 2 - [English]")
    private val embeddedSpanish = PlayerTrack(3, "Track 3", languageCode = "spa")

    private fun pick(preference: SubtitlePreference, embedded: List<PlayerTrack>, externals: List<ExternalSubtitle>) =
        SubtitleAutoPolicy.choose(preference, embedded, externals)?.trackId

    private fun device(language: String, iso3: String) = SubtitleAutoPolicy.preference(null, language, iso3)

    // 선호 언어 정하기

    @Test fun savedPickWinsOverTheDeviceLanguageAndFallsBackAsBefore() {
        assertEquals(SubtitlePreference(setOf("ja"), SubtitleFallback.ANY),
            SubtitleAutoPolicy.preference("ja", "en", "eng"))
        assertEquals(SubtitlePreference(setOf("fre"), SubtitleFallback.ANY),
            SubtitleAutoPolicy.preference("fre", "ko", "kor"))
    }

    @Test fun koreanDeviceOrNoLanguageBehavesAsBefore() {
        assertEquals(SubtitlePreference.saved(null), device("ko", "kor"))
        assertEquals(SubtitlePreference.saved(null), SubtitleAutoPolicy.preference(null, null, null))
        assertEquals(SubtitlePreference.saved(null), SubtitleAutoPolicy.preference("", "und", null))
        assertEquals(setOf(SubtitleAutoPolicy.DEFAULT_LANGUAGE_KEY), SubtitlePreference.saved(null).keys)
    }

    @Test fun otherDeviceLanguagesKeepBothCodesAndFallBackToEnglishOnly() {
        assertEquals(SubtitlePreference(setOf(TrackLanguage.ENGLISH.key), SubtitleFallback.ENGLISH_ONLY),
            device("en", "eng"))
        assertEquals(SubtitlePreference(setOf("es", "spa"), SubtitleFallback.ENGLISH_ONLY), device("es", "spa"))
        assertEquals("옛 인도네시아어 코드도 3글자 코드로 맞춘다", setOf("in", "ind"), device("in", "ind").keys)
        assertEquals(setOf("vi", "vie"), device("vi", "vie").keys)
        assertEquals(setOf("pt", "por"), device("pt", "por").keys)
        assertEquals("3글자 코드를 못 얻어도 2글자로 찾는다", setOf("es"),
            SubtitleAutoPolicy.preference(null, "es", null).keys)
    }

    // 고르기와 대체 규칙

    @Test fun englishDeviceWithOnlyKoreanSubtitlesTurnsNothingOn() {
        assertNull(pick(device("en", "eng"), listOf(embeddedKorean), listOf(koreanFile)))
        assertNull("언어 없는 파일 하나도 켜지 않는다", pick(device("en", "eng"), emptyList(), listOf(loneFile)))
    }

    @Test fun englishDevicePicksEnglish() {
        assertEquals(10, pick(device("en", "eng"), listOf(embeddedKorean), listOf(koreanFile, englishFile)))
        assertEquals(2, pick(device("en", "eng"), listOf(embeddedKorean, embeddedEnglish), listOf(koreanFile)))
    }

    @Test fun otherDeviceLanguageMatchesVlcThreeLetterCodeThenFallsBackToEnglishOnly() {
        assertEquals("spa 트랙이 es 기기와 맞는다", 3,
            pick(device("es", "spa"), listOf(embeddedKorean, embeddedSpanish), listOf(englishFile)))
        assertEquals("스페인어가 없으면 영어 파일", 10,
            pick(device("es", "spa"), listOf(embeddedKorean), listOf(koreanFile, englishFile)))
        assertEquals("영어 파일이 없으면 영어 트랙", 2,
            pick(device("es", "spa"), listOf(embeddedKorean, embeddedEnglish), listOf(koreanFile)))
        assertNull("한국어만 있으면 켜지 않는다",
            pick(device("es", "spa"), listOf(embeddedKorean), listOf(koreanFile, loneFile)))
    }

    @Test fun koreanDeviceAndSavedPickKeepTheKoreanThenEnglishFallback() {
        assertEquals(11, pick(device("ko", "kor"), emptyList(), listOf(englishFile, koreanFile)))
        assertEquals(10, pick(device("ko", "kor"), listOf(embeddedEnglish), listOf(englishFile)))
        assertEquals("저장된 선택은 한국어 트랙으로도 대체한다", 1,
            pick(SubtitleAutoPolicy.preference("ja", "en", "eng"), listOf(embeddedKorean), emptyList()))
    }
}
