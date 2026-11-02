package com.example.torrentstreamplayer

import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ActivityScenario
import com.google.android.gms.ads.AdView
import org.junit.Assert.*
import org.junit.Test

class AdBannerLayoutTest {
    private fun banners(view: View): List<AdView> = when (view) {
        is AdView -> listOf(view)
        is ViewGroup -> (0 until view.childCount).flatMap { banners(view.getChildAt(it)) }
        else -> emptyList()
    }
    private fun awaitBanner(scenario: ActivityScenario<MainActivity>) {
        val deadline = android.os.SystemClock.uptimeMillis() + 20000
        while (true) {
            var found = false
            scenario.onActivity { found = banners(it.window.decorView).any { ad -> ad.height > 0 } }
            if (found) return
            check(android.os.SystemClock.uptimeMillis() < deadline) { "Banner did not attach" }
            Thread.sleep(100)
        }
    }
    private fun verifyInsets(scenario: ActivityScenario<MainActivity>) {
        scenario.onActivity { activity ->
            val root = activity.window.decorView
            val banner = banners(root).single()
            val position = IntArray(2)
            banner.getLocationInWindow(position)
            val navigation = ViewCompat.getRootWindowInsets(root)!!
                .getInsets(WindowInsetsCompat.Type.navigationBars())
            assertEquals("ca-app-pub-3940256099942544/9214589741", banner.adUnitId)
            assertTrue("Banner overlaps system navigation", position[1] + banner.height <= root.height - navigation.bottom)
            assertTrue("Banner overlaps side navigation", position[0] + banner.width <= root.width - navigation.right)
        }
    }
    @Test fun demoBannerStaysInsideSystemInsetsInBothOrientations() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            awaitBanner(scenario)
            verifyInsets(scenario)
            scenario.onActivity {
                it.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
            val deadline = android.os.SystemClock.uptimeMillis() + 10000
            while (true) {
                var landscape = false
                scenario.onActivity {
                    landscape = it.resources.configuration.orientation ==
                        android.content.res.Configuration.ORIENTATION_LANDSCAPE
                }
                if (landscape) break
                check(android.os.SystemClock.uptimeMillis() < deadline)
                Thread.sleep(100)
            }
            Thread.sleep(500)
            awaitBanner(scenario)
            verifyInsets(scenario)
        }
    }
}
