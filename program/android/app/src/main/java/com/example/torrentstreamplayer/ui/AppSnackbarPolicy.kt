package com.example.torrentstreamplayer.ui

import com.example.torrentstreamplayer.ui.navigation.AppScreen

/**
 * 앱 공용 스낵바(받기 시작 Wi-Fi 대기 안내)를 그릴 화면. 재생·준비 화면에서는 그리지 않는다: 영상 아래쪽 조작(탐색
 * 막대)을 가리고 그 영역의 탭을 막기 때문이다. 화면이 바뀌면 떠 있던 스낵바는 `AppRoot`가 닫는다.
 */
internal object AppSnackbarPolicy {
    fun visible(screen: AppScreen): Boolean = when (screen) {
        is AppScreen.Main, AppScreen.Playlist, AppScreen.StoredFiles, AppScreen.OpenSourceLicenses,
        AppScreen.Settings -> true
        is AppScreen.Preparing, is AppScreen.Player -> false
    }
}
