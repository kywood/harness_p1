package com.example.torrentstreamplayer.ui

import com.example.torrentstreamplayer.ui.navigation.AppScreen

internal object AppBackPolicy {
    /** 준비·재생·오류 화면의 뒤로 가기는 앱을 닫지 않고 재생을 닫는다. 목록 화면은 각자 뒤로 가기를 처리한다. */
    fun returnsToInput(screen: AppScreen): Boolean = when (screen) {
        is AppScreen.Main -> screen.errorMessage != null
        is AppScreen.Preparing, is AppScreen.Player -> true
        AppScreen.Playlist, AppScreen.StoredFiles -> false
    }
}
