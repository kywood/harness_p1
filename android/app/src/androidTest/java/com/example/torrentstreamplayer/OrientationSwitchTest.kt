package com.example.torrentstreamplayer

import android.content.res.Configuration
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.torrentstreamplayer.ui.MainViewModel
import com.example.torrentstreamplayer.ui.VideoOrientation
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OrientationSwitchTest {
    private fun awaitOrientation(scenario: ActivityScenario<MainActivity>, expected: Int) {
        val deadline = System.nanoTime() + 8_000_000_000L
        while (System.nanoTime() < deadline) {
            var actual = 0
            scenario.onActivity { actual = it.resources.configuration.orientation }
            if (actual == expected) return
            Thread.sleep(50)
        }
        fail("Requested orientation did not arrive")
    }
    @Test fun rotateBothWaysKeepsActivityAndViewModel() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var initial: MainActivity
            lateinit var model: MainViewModel
            scenario.onActivity {
                initial = it; model = ViewModelProvider(it)[MainViewModel::class.java]
                it.requestedOrientation = VideoOrientation.next(false)
            }
            awaitOrientation(scenario, Configuration.ORIENTATION_LANDSCAPE)
            scenario.onActivity {
                assertSame(initial, it)
                assertSame(model, ViewModelProvider(it)[MainViewModel::class.java])
                it.requestedOrientation = VideoOrientation.next(true)
            }
            awaitOrientation(scenario, Configuration.ORIENTATION_PORTRAIT)
            scenario.onActivity { assertSame(initial, it) }
        }
    }
}
