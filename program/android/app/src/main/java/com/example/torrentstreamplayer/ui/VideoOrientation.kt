package com.example.torrentstreamplayer.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.annotation.StringRes
import com.example.torrentstreamplayer.R

internal object VideoOrientation {
    /** Reading name of the rotate button: what pressing it switches to. */
    @StringRes fun description(landscape: Boolean): Int =
        if (landscape) R.string.cd_orientation_portrait else R.string.cd_orientation_landscape
    fun next(landscape: Boolean): Int = if (landscape) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        else ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    tailrec fun activity(context: Context): Activity? = when (context) {
        is Activity -> context
        is ContextWrapper -> activity(context.baseContext)
        else -> null
    }
}
