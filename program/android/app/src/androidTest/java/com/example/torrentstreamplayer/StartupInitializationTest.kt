package com.example.torrentstreamplayer

import android.app.Application
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.semantics.getAllSemanticsNodes
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
                fun roots(view: android.view.View): List<androidx.compose.ui.node.RootForTest> = when (view) {
                    is androidx.compose.ui.node.RootForTest -> listOf(view)
                    is android.view.ViewGroup -> (0 until view.childCount).flatMap { roots(view.getChildAt(it)) }
                    else -> emptyList()
                }
                assertTrue(roots(activity.window.decorView).flatMap { it.semanticsOwner.getAllSemanticsNodes(true) }
                    .any { it.config.getOrNull(androidx.compose.ui.semantics.SemanticsProperties.ContentDescription)
                        ?.contains("PiecePlay 로고") == true })
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
