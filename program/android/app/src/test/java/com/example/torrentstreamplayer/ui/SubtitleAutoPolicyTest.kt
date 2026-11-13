package com.example.torrentstreamplayer.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class SubtitleAutoPolicyTest {
    @Test fun savedPickWinsOverTheDeviceLanguage() {
        assertEquals("ja", SubtitleAutoPolicy.preferredKey("ja", "en"))
        assertEquals("fre", SubtitleAutoPolicy.preferredKey("fre", "ko"))
    }

    @Test fun withoutAPickTheDeviceLanguageIsPreferred() {
        assertEquals(TrackLanguage.ENGLISH.key, SubtitleAutoPolicy.preferredKey(null, "en"))
        assertEquals("영어 기기에 저장 값이 비어 있으면 기기 언어", TrackLanguage.ENGLISH.key,
            SubtitleAutoPolicy.preferredKey("", "en"))
        assertEquals("등록되지 않은 언어는 코드 그대로", "es", SubtitleAutoPolicy.preferredKey(null, "es"))
    }

    @Test fun koreanDeviceStillPrefersKorean() {
        assertEquals(TrackLanguage.KOREAN.key, SubtitleAutoPolicy.preferredKey(null, "ko"))
    }

    @Test fun unreadableDeviceLanguageFallsBackToTheDefault() {
        assertEquals(SubtitleAutoPolicy.DEFAULT_LANGUAGE_KEY, SubtitleAutoPolicy.preferredKey(null, null))
        assertEquals(SubtitleAutoPolicy.DEFAULT_LANGUAGE_KEY, SubtitleAutoPolicy.preferredKey(null, "und"))
    }
}
