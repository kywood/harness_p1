package com.example.torrentstreamplayer

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.ViewModelProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.torrentstreamplayer.ui.MainViewModel
import com.example.torrentstreamplayer.ui.navigation.AppScreen
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** 메인 하단 "오픈소스 라이선스" → 목록 → 전문 → 뒤로 → 목록 → 뒤로 → 메인(`design/specs/main-legal-footer.md` 2절). */
@RunWith(AndroidJUnit4::class)
class OpenSourceLicensesFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun text(id: Int): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun awaitText(value: String) {
        compose.waitUntil(5_000) { compose.onAllNodesWithText(value).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun screen(): AppScreen = ViewModelProvider(compose.activity)[MainViewModel::class.java].screen.value

    @Test fun footerLinkOpensListThenTextAndBackReturnsToMain() {
        awaitText(text(R.string.main_rights_notice))
        compose.onNodeWithText(text(R.string.action_open_source_licenses)).performScrollTo().performClick()

        awaitText(text(R.string.licenses_intro))
        compose.runOnIdle { assertTrue(screen() == AppScreen.OpenSourceLicenses) }
        compose.onNodeWithText("LibVLC").performScrollTo().performClick()

        awaitText(text(R.string.licenses_lgpl_notice))
        compose.onAllNodesWithText(text(R.string.licenses_intro)).fetchSemanticsNodes()
            .let { assertTrue("전문에서는 목록 안내가 보이지 않는다", it.isEmpty()) }

        Espresso.pressBack()
        awaitText(text(R.string.licenses_intro))
        compose.runOnIdle { assertTrue(screen() == AppScreen.OpenSourceLicenses) }

        Espresso.pressBack()
        awaitText(text(R.string.main_rights_notice))
        compose.runOnIdle {
            assertTrue(screen() is AppScreen.Main)
            assertFalse(compose.activity.isFinishing)
        }
    }
}
