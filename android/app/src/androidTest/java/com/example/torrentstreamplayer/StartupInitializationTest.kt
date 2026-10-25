package com.example.torrentstreamplayer

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.torrentstreamplayer.ui.MainViewModel
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StartupInitializationTest {
    @Test fun idleActivityDoesNotInitializeTorrentEngine() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { activity ->
                assertTrue(activity.window.attributes.flags and android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0)
                val model = ViewModelProvider(activity)[MainViewModel::class.java]
                assertFalse(model.isTorrentEngineInitialized)
                model.stopStream()
                assertFalse(model.isTorrentEngineInitialized)
            }
        }
    }
    @Test fun engineIsCreatedOnlyWhenRequestedAndReused() {
        val application = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application
        val model = MainViewModel(application)
        assertFalse(model.isTorrentEngineInitialized)
        val engine = model.torrentEngine
        assertTrue(model.isTorrentEngineInitialized)
        assertSame(engine, model.torrentEngine)
        model.stopStream()
    }
}
