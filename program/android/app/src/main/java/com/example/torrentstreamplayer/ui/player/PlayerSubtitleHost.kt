package com.example.torrentstreamplayer.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import com.example.torrentstreamplayer.engine.SubtitleFile
import androidx.compose.ui.platform.LocalContext
import com.example.torrentstreamplayer.ui.EngineCommandQueue
import com.example.torrentstreamplayer.ui.PlaybackClock
import com.example.torrentstreamplayer.ui.SubtitleSession
import com.example.torrentstreamplayer.ui.SubtitleSheetState
import org.videolan.libvlc.MediaPlayer
import java.io.File

/**
 * 자막 시트에 필요한 값(`design/specs/subtitles.md`). [files]·[pending]은 엔진이 받은(검증된)·받는 중인 자막 파일,
 * [autoSelect]는 설정 "자막 자동 표시", [preferredLanguage]는 기억한 선호 언어. 고른 언어는 [onLanguageChosen]으로 알린다.
 */
internal data class PlayerSubtitleOptions(
    val files: List<SubtitleFile> = emptyList(),
    val pending: List<File> = emptyList(),
    val autoSelect: Boolean = true,
    val preferredLanguage: String? = null,
    val onLanguageChosen: (String) -> Unit = {},
)

/**
 * 재생기 하나의 자막 연결. [player]마다 한 번 만들고(그때의 설정을 쓴다, 재생 중 바뀐 설정은 다음 재생부터),
 * [running](재생 중이고 버퍼링이 아님)·[trackChanges](VLC 트랙 이벤트 수)·엔진 자막 파일이 바뀔 때 세션에 알린다.
 * [running]인 동안만 자막 파일 대기 시간을 센다([PlaybackClock]). CP949 자막의 UTF-8 사본([SubtitleCopies])은 앱 캐시
 * `subtitle_copies/`에 IO 디스패처에서 만들고(첫 사본 전에 이전 재생의 남은 사본을 지운다), [PlayerSubtitles.close] 때
 * 지운다. 재생기를 놓기 전에 호출하는 쪽이 [PlayerSubtitles.close]를 부른다.
 */
@Composable
internal fun rememberPlayerSubtitles(
    player: MediaPlayer?,
    options: PlayerSubtitleOptions,
    running: Boolean,
    trackChanges: Int,
): PlayerSubtitles? {
    val scope = rememberCoroutineScope()
    val languageChosen by rememberUpdatedState(options.onLanguageChosen)
    val cacheDir = LocalContext.current.cacheDir
    val clock = remember(player) { PlaybackClock() }
    val copies = remember(player) { SubtitleCopies(File(cacheDir, COPY_DIR)) }
    val subtitles = remember(player) {
        player?.let {
            val session = SubtitleSession(VlcTrackPlayer(it), options.autoSelect, options.preferredLanguage,
                clock::elapsedMs, copies::copyOf)
            PlayerSubtitles(session, scope, EngineCommandQueue.shared.dispatcher, copies) { key ->
                languageChosen(key)
            }
        }
    }
    LaunchedEffect(clock, running) { clock.setRunning(running) }
    LaunchedEffect(subtitles, running) { if (running) subtitles?.start() }
    LaunchedEffect(subtitles, trackChanges) { if (trackChanges > 0) subtitles?.refresh() }
    LaunchedEffect(subtitles, options.files, options.pending) { subtitles?.updateFiles(options.files, options.pending) }
    return subtitles
}

/** 자막 사본 폴더 이름(`cacheDir` 아래, 다운로드 폴더 밖). */
private const val COPY_DIR = "subtitle_copies"

/** 지금 시트 상태. 재생기가 없으면 빈 상태다. */
@Composable
internal fun PlayerSubtitles?.sheetState(): SubtitleSheetState =
    this?.sheet?.collectAsState()?.value ?: SubtitleSheetState()

/** 자막 시트가 [open]일 때 그린다. 닫히면 아직 오지 않은 선택 실패 알림을 버린다(리뷰 13). */
@Composable
internal fun PlayerSubtitleSheetHost(subtitles: PlayerSubtitles?, open: Boolean, onDismiss: () -> Unit) {
    if (subtitles == null) return
    DisposableEffect(subtitles, open) {
        subtitles.sheetShown(open)
        onDispose { subtitles.sheetShown(false) }
    }
    if (open) {
        SubtitleSheet(subtitles.sheetState(), onDismiss, { subtitles.selectSubtitle(it) },
            { subtitles.selectAudio(it) }, subtitles.selectFailed)
    }
}
