package com.example.torrentstreamplayer

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

/**
 * 메인의 로고·앱 이름·한 줄 설명은 가로 가운데에, 설정 톱니는 오른쪽 위에 있다(#182).
 * 톱니 줄을 넣으면서 묶음 Column의 가로 정렬이 빠져 헤더가 왼쪽에 붙었던 회귀를 막는다.
 */
@RunWith(AndroidJUnit4::class)
class MainBrandHeaderLayoutTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun text(id: Int): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    @Test fun brandHeaderIsHorizontallyCenteredAndGearStaysRight() {
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText(text(R.string.main_tagline)).fetchSemanticsNodes().isNotEmpty()
        }
        val screenCenter = compose.onRoot().fetchSemanticsNode().boundsInRoot.center.x
        // 반올림 오차만 허용한다.
        val tolerance = 2f
        val header = listOf(
            compose.onNodeWithContentDescription(text(R.string.cd_app_logo)),
            compose.onNodeWithText(text(R.string.app_name)),
            compose.onNodeWithText(text(R.string.main_tagline))
        )
        header.forEach { node ->
            val center = node.fetchSemanticsNode().boundsInRoot.center.x
            assertTrue("header center $center, screen center $screenCenter", abs(center - screenCenter) <= tolerance)
        }
        val gear = compose.onNodeWithContentDescription(text(R.string.title_settings)).fetchSemanticsNode().boundsInRoot
        assertTrue("gear should stay on the right: $gear", gear.left > screenCenter)
    }
}
