package com.example.torrentstreamplayer.ui.navigation

import com.example.torrentstreamplayer.ui.StreamUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 사용자가 고른 목적지. [MAIN]이면 메인·준비·재생 중 무엇을 보일지는 스트림 상태가 정한다. */
internal enum class Destination { MAIN, PLAYLIST, STORED_FILES }

/** 재생을 시작한 곳. 재생·준비를 닫으면 [closeTo]로 돌아간다. */
internal enum class PlaybackOrigin(val closeTo: Destination) {
    MAIN(Destination.MAIN),
    PLAYLIST(Destination.PLAYLIST),
}

/** 화면 이동 상태. 함께 바뀌는 값이라 하나로 묶어 한 번에 교체한다. */
internal data class NavigationState(
    val destination: Destination = Destination.MAIN,
    val origin: PlaybackOrigin = PlaybackOrigin.MAIN,
)

/**
 * 화면 이동을 맡는 순수 객체. 이동 의도(열기·닫기·재생 시작)와 스트림 상태를 받아 [screen]을 하나로 정한다.
 * ViewModel의 코루틴과 테스트 스레드에서 함께 불릴 수 있어 상태 변경은 동기화한다.
 */
internal class AppNavigator(stream: StreamUiState = StreamUiState.Idle) {
    private var navigation = NavigationState()
    private var stream = stream
    private val _screen = MutableStateFlow(screenOf(navigation, stream))
    val screen: StateFlow<AppScreen> = _screen.asStateFlow()
    val origin: PlaybackOrigin @Synchronized get() = navigation.origin

    @Synchronized fun onStreamChanged(state: StreamUiState) {
        stream = state
        publish()
    }

    fun openPlaylist() = moveTo(navigation.copy(destination = Destination.PLAYLIST))

    fun openStoredFiles() = moveTo(navigation.copy(destination = Destination.STORED_FILES))

    /** 재생 목록·저장 파일 관리에서 뒤로 가면 메인 흐름으로 돌아간다. */
    fun closeList() = moveTo(navigation.copy(destination = Destination.MAIN))

    fun startPlayback(origin: PlaybackOrigin) = moveTo(NavigationState(Destination.MAIN, origin))

    /** 재생·준비를 닫고 시작한 곳으로 돌아간다. 돌아간 곳을 정한 출발점을 돌려준다. */
    @Synchronized fun closePlayback(): PlaybackOrigin {
        val origin = navigation.origin
        moveTo(NavigationState(origin.closeTo))
        return origin
    }

    /** 준비 화면의 "메인": 어디서 시작했든 메인으로 간다. */
    fun cancelToMain() = moveTo(NavigationState())

    /** 준비 화면의 "재생 목록" 또는 재생 완료: 재생 목록으로 간다. */
    fun showPlaylistAfterPlayback() = moveTo(NavigationState(Destination.PLAYLIST))

    @Synchronized private fun moveTo(next: NavigationState) {
        navigation = next
        publish()
    }

    private fun publish() {
        _screen.value = screenOf(navigation, stream)
    }

    private companion object {
        fun screenOf(navigation: NavigationState, stream: StreamUiState): AppScreen = when (navigation.destination) {
            Destination.PLAYLIST -> AppScreen.Playlist
            Destination.STORED_FILES -> AppScreen.StoredFiles
            Destination.MAIN -> when (stream) {
                is StreamUiState.Idle -> AppScreen.Main()
                is StreamUiState.Error -> AppScreen.Main(stream.message)
                is StreamUiState.FetchingMetadata -> AppScreen.Preparing.FetchingMetadata
                is StreamUiState.Buffering -> AppScreen.Preparing.Buffering(stream)
                is StreamUiState.Playing -> AppScreen.Player(stream.fileName)
            }
        }
    }
}
