package com.example.torrentstreamplayer.ui.navigation

import com.example.torrentstreamplayer.ui.StreamUiState

/** 지금 보여 줄 화면. [AppNavigator]가 이동 상태와 [StreamUiState]를 합쳐 하나로 정한다. */
sealed interface AppScreen {
    /** 메인(링크 입력). [errorMessage]가 있으면 입력 오류를 함께 보여 준다. */
    data class Main(val errorMessage: String? = null) : AppScreen

    /** 재생 목록. */
    data object Playlist : AppScreen

    /** 저장 파일 관리. */
    data object StoredFiles : AppScreen

    /** 오픈소스 라이선스(목록과 전문 보기). 목록·전문 전환은 화면 안에서 한다. */
    data object OpenSourceLicenses : AppScreen

    /** 설정(Wi-Fi 전용, 자막 자동 표시, 정보). */
    data object Settings : AppScreen

    /** 준비 화면: 토렌트 정보 가져오기와 버퍼링. */
    sealed interface Preparing : AppScreen {
        data object FetchingMetadata : Preparing
        data class Buffering(val stream: StreamUiState.Buffering) : Preparing
    }

    /** 재생 화면. */
    data class Player(val fileName: String) : AppScreen
}
