package com.example.torrentstreamplayer.ui.screens

import androidx.annotation.StringRes
import com.example.torrentstreamplayer.R
import com.example.torrentstreamplayer.ui.navigation.AppScreen

/** 준비 화면의 단계 표시(정보 → 연결 → 버퍼링). 화면 상태를 바꾸지 않고 보여 줄 단계만 고른다. */
internal enum class PreparingStep(@get:StringRes val label: Int) {
    INFO(R.string.preparing_step_info),
    CONNECT(R.string.preparing_step_connect),
    BUFFERING(R.string.label_buffering);

    companion object {
        /** 토렌트 정보를 받기 전은 정보, 받은 뒤 영상 준비 중은 연결, 버퍼링 상태는 버퍼링. */
        fun of(screen: AppScreen.Preparing, hasMetadata: Boolean): PreparingStep = when (screen) {
            AppScreen.Preparing.FetchingMetadata -> if (hasMetadata) CONNECT else INFO
            is AppScreen.Preparing.Buffering -> BUFFERING
        }
    }
}
