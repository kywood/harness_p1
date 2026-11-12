package com.example.torrentstreamplayer.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.torrentstreamplayer.ui.AppBackPolicy
import com.example.torrentstreamplayer.ui.DownloadPhase
import com.example.torrentstreamplayer.ui.MagnetHistoryScreen
import com.example.torrentstreamplayer.ui.MainViewModel
import com.example.torrentstreamplayer.ui.PlaylistFilter
import com.example.torrentstreamplayer.ui.PlaylistSort
import com.example.torrentstreamplayer.ui.QuickDownloadDialog
import com.example.torrentstreamplayer.ui.StoredFilesScreen
import com.example.torrentstreamplayer.ui.TestAdPolicy
import com.example.torrentstreamplayer.ui.TestBannerAd
import com.example.torrentstreamplayer.ui.VlcPlayerScreen
import com.example.torrentstreamplayer.ui.navigation.AppScreen

/**
 * 앱의 최상위 화면. [MainViewModel.screen]에 따라 화면 하나를 고르고, 사용자 입력을 ViewModel 의도 메서드로 넘긴다.
 *
 * @param onPrivacyOptions 광고 개인정보 설정이 필요할 때만 넘긴다.
 * @param onPlaybackFinished 재생을 끝내거나 끝까지 본 뒤 불린다. 시청 시간(ms)으로 전면 광고를 판단한다.
 */
@Composable
internal fun AppRoot(
    viewModel: MainViewModel,
    onPrivacyOptions: (() -> Unit)?,
    onPlaybackFinished: (watchedMs: Long) -> Unit
) {
    val screen by viewModel.screen.collectAsState()
    var showDownloadDialog by remember { mutableStateOf(false) }
    var simplePlaylist by rememberSaveable { mutableStateOf(false) }
    var playlistSort by rememberSaveable { mutableStateOf(PlaylistSort.RECENT_PLAY) }
    var playlistFilter by rememberSaveable { mutableStateOf(PlaylistFilter.ALL) }
    val downloadJobs by viewModel.downloads.tasks.collectAsState()
    val downloadError by viewModel.downloads.error.collectAsState()
    if (showDownloadDialog) QuickDownloadDialog(onDismiss = { showDownloadDialog = false }, submit = viewModel::queueDownload)
    BackHandler(enabled = AppBackPolicy.returnsToInput(screen)) { viewModel.closePlayback() }

    Scaffold(modifier = Modifier.fillMaxSize(), bottomBar = {
        if (TestAdPolicy.visible(screen)) Box(Modifier.navigationBarsPadding()) { TestBannerAd() }
    }) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (val current = screen) {
                AppScreen.StoredFiles -> StoredFilesScreen(viewModel::storedFiles, viewModel::deleteStoredFile,
                    onBack = viewModel::closeList)
                AppScreen.Playlist -> MagnetHistoryScreen(viewModel::savedMagnets, viewModel::deleteSavedMagnet,
                    onPlay = viewModel::playFromPlaylist,
                    onBack = viewModel::closeList, onDownload = { viewModel.startStreaming(it, downloadOnly = true) },
                    onStopDownload = viewModel::stopDownloads, downloadJobs = downloadJobs,
                    onStopItem = viewModel::stopDownload, onStopItems = viewModel::stopDownloads,
                    downloadError = downloadError,
                    simple = simplePlaylist, onSimpleChange = { simplePlaylist = it },
                    downloadCount = downloadJobs.values.count { it.phase == DownloadPhase.RUNNING },
                    sort = playlistSort, onSortChange = { playlistSort = it },
                    filter = playlistFilter, onFilterChange = { playlistFilter = it },
                    onAddDownload = { showDownloadDialog = true }, onResetPosition = viewModel::resetPlaybackPosition)
                is AppScreen.Main -> MagnetLinkInputScreen(
                    onStartStream = viewModel::playFromMain,
                    errorMessage = current.errorMessage,
                    onOpenFiles = viewModel::openStoredFiles,
                    onOpenHistory = viewModel::openPlaylist,
                    onPrivacyOptions = onPrivacyOptions
                )
                is AppScreen.Preparing -> {
                    val metadata by viewModel.metadataDetails.collectAsState()
                    val pieces by viewModel.pieceMap.collectAsState()
                    PreparingScreen(current, metadata, pieces,
                        onCancel = viewModel::cancelToMain, onOpenPlaylist = viewModel::continueInPlaylist)
                }
                is AppScreen.Player -> {
                    // TODO(#37 이후, 디자인 4단계): 재생 화면 분리 때 엔진 대신 재생 포트를 넘긴다.
                    val engine = viewModel.torrentEngine
                    val positionWriter = remember(engine) { viewModel.playbackPositionWriter() }
                    // Read once per player: the saved position changes every few seconds while playing.
                    val startPositionMs = remember(engine) { viewModel.resumePositionMs }
                    VlcPlayerScreen(
                        fileName = current.fileName,
                        torrentEngine = engine,
                        onBackClicked = viewModel::closePlayback,
                        onPlaybackExited = { watchedMs ->
                            viewModel.closePlayback()
                            onPlaybackFinished(watchedMs)
                        },
                        onAddDownload = { showDownloadDialog = true },
                        initialPositionMs = startPositionMs, onPositionChanged = positionWriter,
                        onPlaybackCompleted = { watchedMs ->
                            viewModel.onPlaybackCompleted()
                            onPlaybackFinished(watchedMs)
                        }
                    )
                }
            }
        }
    }
}
