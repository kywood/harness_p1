package com.example.torrentstreamplayer
import android.content.pm.ActivityInfo
import com.example.torrentstreamplayer.ui.VideoOrientation
import org.junit.Assert.*
import org.junit.Test

class VideoOrientationTest {
    @Test fun portraitButtonRequestsSensorLandscape() {
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, VideoOrientation.next(false))
        assertEquals(R.string.cd_orientation_landscape, VideoOrientation.description(false))
    }
    @Test fun landscapeButtonReturnsToPortrait() {
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, VideoOrientation.next(true))
        assertEquals(R.string.cd_orientation_portrait, VideoOrientation.description(true))
    }
}
