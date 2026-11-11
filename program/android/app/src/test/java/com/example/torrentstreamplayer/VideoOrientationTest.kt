package com.example.torrentstreamplayer
import android.content.pm.ActivityInfo
import com.example.torrentstreamplayer.ui.VideoOrientation
import org.junit.Assert.*
import org.junit.Test

class VideoOrientationTest {
    @Test fun portraitButtonRequestsSensorLandscape() {
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, VideoOrientation.next(false))
        assertEquals("가로 화면으로 전환", VideoOrientation.description(false))
    }
    @Test fun landscapeButtonReturnsToPortrait() {
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, VideoOrientation.next(true))
        assertEquals("세로 화면으로 전환", VideoOrientation.description(true))
    }
}
