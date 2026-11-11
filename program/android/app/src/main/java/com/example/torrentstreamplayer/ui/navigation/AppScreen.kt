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

    /** 준비 화면: 토렌트 정보 가져오기와 버퍼링. */
    sealed interface Preparing : AppScreen {
        data object FetchingMetadata : Preparing
        data class Buffering(val stream: StreamUiState.Buffering) : Preparing
    }

    /** 재생 화면. */
    data class Player(val fileName: String) : AppScreen
}
