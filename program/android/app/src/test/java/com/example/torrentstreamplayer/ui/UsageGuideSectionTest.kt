package com.example.torrentstreamplayer.ui

import com.example.torrentstreamplayer.StringsXml
import com.example.torrentstreamplayer.ui.navigation.AppScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageGuideSectionTest {
    private val strings: Map<String, String> by lazy { StringsXml.texts("values-ko") }
    private val english: Map<String, String> by lazy { StringsXml.texts(StringsXml.DEFAULT) }

    @Test fun sectionsFollowTheSpecOrder() {
        assertEquals(
            listOf(UsageGuideSection.WHAT, UsageGuideSection.UPLOAD, UsageGuideSection.STORAGE,
                UsageGuideSection.DATA, UsageGuideSection.ADS),
            UsageGuideSection.entries
        )
    }

    @Test fun everySectionHasItsOwnTitleAndBody() {
        val ids = UsageGuideSection.entries.flatMap { listOf(it.title, it.body) }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test fun guideKeysAreRegisteredInStrings() {
        val keys = UsageGuideSection.entries.flatMap { section ->
            val name = "guide_" + section.name.lowercase()
            listOf("${name}_title", "${name}_body")
        } + listOf("guide_rights_body", "main_rights_notice", "action_usage_guide", "action_confirm")
        keys.forEach { assertTrue("$it is missing", strings[it].orEmpty().isNotBlank()) }
        keys.forEach { assertTrue("$it is missing in English", english[it].orEmpty().isNotBlank()) }
    }

    /** 업로드하지 않는다는 장점과 IP 노출을 같은 절에 둔다(시안 C 2절 문구 점검). */
    @Test fun uploadSectionAlsoMentionsIpExposure() {
        assertTrue(strings.getValue("guide_upload_body").contains("IP 주소"))
        assertTrue(english.getValue("guide_upload_body").contains("IP address"))
    }

    /** 광고 규칙이 "재생 중 광고"를 허용하게 바뀌면 이 문장도 고쳐야 한다(시안 C program 체크 항목). */
    @Test fun adsSectionSaysNoAdsDuringPlayback() {
        assertTrue(strings.getValue("guide_ads_body").contains("재생하는 동안에는 광고가 나오지 않습니다"))
        assertTrue(english.getValue("guide_ads_body").contains("No ads appear during playback"))
        assertFalse(TestAdPolicy.visible(AppScreen.Player("video")))
    }
}
