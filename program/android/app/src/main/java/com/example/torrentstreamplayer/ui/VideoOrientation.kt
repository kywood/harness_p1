package com.example.torrentstreamplayer.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo

internal object VideoOrientation {
    fun description(landscape: Boolean): String = if (landscape) "세로 화면으로 전환" else "가로 화면으로 전환"
    fun next(landscape: Boolean): Int = if (landscape) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        else ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    tailrec fun activity(context: Context): Activity? = when (context) {
        is Activity -> context
        is ContextWrapper -> activity(context.baseContext)
        else -> null
    }
}
