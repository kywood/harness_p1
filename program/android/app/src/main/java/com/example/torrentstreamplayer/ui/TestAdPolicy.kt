package com.example.torrentstreamplayer.ui

import com.example.torrentstreamplayer.meta.AdUnit
import com.example.torrentstreamplayer.ui.navigation.AppScreen

internal object TestAdPolicy {
    /** Google's test banner unit; kept as a name for existing callers. */
    val BANNER_UNIT_ID: String get() = AdUnit.BANNER.debugId
    fun bannerUnitId(debug: Boolean): String = AdUnit.BANNER.id(debug)
    /** 배너는 메인과 재생 목록에만 둔다. */
    fun visible(screen: AppScreen): Boolean = when (screen) {
        is AppScreen.Main, AppScreen.Playlist -> true
        AppScreen.StoredFiles, AppScreen.OpenSourceLicenses, is AppScreen.Preparing, is AppScreen.Player -> false
    }
}
