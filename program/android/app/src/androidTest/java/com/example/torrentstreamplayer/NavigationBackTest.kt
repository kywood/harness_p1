package com.example.torrentstreamplayer
import androidx.test.core.app.ActivityScenario
import androidx.lifecycle.ViewModelProvider
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.torrentstreamplayer.ui.MainViewModel
import com.example.torrentstreamplayer.ui.StreamUiState
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationBackTest {
    private fun awaitFrame(scenario: ActivityScenario<MainActivity>) {
        val signal = CountDownLatch(1)
        scenario.onActivity { activity -> activity.window.decorView.postOnAnimation {
            activity.window.decorView.postOnAnimation { signal.countDown() }
        } }
        assertTrue(signal.await(5, TimeUnit.SECONDS))
    }
    @Test fun systemBackFromErrorReturnsToInputWithoutFinishingActivity() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { ViewModelProvider(it)[MainViewModel::class.java].startStreaming("") }
            awaitFrame(scenario)
            scenario.onActivity {
                assertTrue(it.onBackPressedDispatcher.hasEnabledCallbacks())
                it.onBackPressedDispatcher.onBackPressed()
                assertFalse(it.isFinishing)
                assertEquals(StreamUiState.Idle, ViewModelProvider(it)[MainViewModel::class.java].uiState.value)
            }
        }
    }
}
