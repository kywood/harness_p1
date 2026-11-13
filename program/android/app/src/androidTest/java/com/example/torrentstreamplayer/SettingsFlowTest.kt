package com.example.torrentstreamplayer

import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.ViewModelProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.torrentstreamplayer.data.SettingsRepository
import com.example.torrentstreamplayer.ui.MainViewModel
import com.example.torrentstreamplayer.ui.navigation.AppScreen
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 메인 오른쪽 위 톱니 → 설정 → 뒤로 → 메인(`design/specs/settings-wifi.md` 1·2절). Wi-Fi 전용 스위치를 바꾸면
 * 화면을 다시 열어도, 디스크에서 새로 읽어도 바뀐 값이 남는다. 끝나면 원래 값으로 되돌린다.
 */
@RunWith(AndroidJUnit4::class)
class SettingsFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private var originalWifiOnly: Boolean? = null

    private fun text(id: Int): String = context.getString(id)

    private fun awaitText(value: String) {
        compose.waitUntil(5_000) { compose.onAllNodesWithText(value).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun viewModel(): MainViewModel = ViewModelProvider(compose.activity)[MainViewModel::class.java]

    private fun savedWifiOnly(): Boolean = runBlocking {
        SettingsRepository.create(context).run {
            load()
            settings.value.wifiOnly
        }
    }

    @After fun restore() {
        val original = originalWifiOnly ?: return
        compose.runOnUiThread { viewModel().setWifiOnly(original) }
        compose.waitUntil(5_000) { savedWifiOnly() == original }
    }

    @Test fun gearOpensSettingsBackReturnsToMainAndSwitchIsSaved() {
        awaitText(text(R.string.main_rights_notice))
        compose.onNodeWithContentDescription(text(R.string.title_settings)).performClick()
        awaitText(text(R.string.settings_section_download))
        compose.runOnIdle { assertEquals(AppScreen.Settings, viewModel().screen.value) }

        val before = compose.runOnIdle { viewModel().settings.value.wifiOnly }
        originalWifiOnly = before
        val wifiRow = compose.onNodeWithText(text(R.string.settings_wifi_only), substring = true, useUnmergedTree = false)
        if (before) wifiRow.assertIsOn() else wifiRow.assertIsOff()
        wifiRow.performClick()
        compose.waitUntil(5_000) { viewModel().settings.value.wifiOnly == !before }
        compose.waitUntil(5_000) { savedWifiOnly() == !before }

        Espresso.pressBack()
        awaitText(text(R.string.main_rights_notice))
        compose.runOnIdle {
            assertTrue(viewModel().screen.value is AppScreen.Main)
            assertFalse(compose.activity.isFinishing)
        }

        compose.onNodeWithContentDescription(text(R.string.title_settings)).performClick()
        awaitText(text(R.string.settings_section_download))
        val reopened = compose.onNodeWithText(text(R.string.settings_wifi_only), substring = true)
        if (before) reopened.assertIsOff() else reopened.assertIsOn()

        compose.onNodeWithText(text(R.string.action_open_source_licenses)).performScrollTo().performClick()
        awaitText(text(R.string.licenses_intro))
        Espresso.pressBack()
        awaitText(text(R.string.settings_section_download))
        compose.runOnIdle { assertEquals(AppScreen.Settings, viewModel().screen.value) }
    }
}
