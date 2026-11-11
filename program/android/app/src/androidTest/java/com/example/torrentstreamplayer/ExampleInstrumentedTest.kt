package com.example.torrentstreamplayer

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test fun launcherIconLoadsAsAdaptiveArtwork() {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val icon = app.applicationInfo.loadIcon(app.packageManager)
        assertTrue(icon is android.graphics.drawable.AdaptiveIconDrawable)
        val bitmap = android.graphics.Bitmap.createBitmap(512, 512, android.graphics.Bitmap.Config.ARGB_8888)
        icon.setBounds(0, 0, 512, 512); icon.draw(android.graphics.Canvas(bitmap))
        java.io.File(app.cacheDir, "icon-preview.png").outputStream().use {
            assertTrue(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it))
        }
    }
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.gamenuri.pieceplay", appContext.packageName)
        assertEquals("PiecePlay", appContext.getString(appContext.applicationInfo.labelRes))
    }
}
