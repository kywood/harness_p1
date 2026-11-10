package com.example.torrentstreamplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.torrentstreamplayer.ui.MainViewModel
import com.example.torrentstreamplayer.ui.StreamUiState
import com.example.torrentstreamplayer.ui.VlcPlayerScreen
import com.example.torrentstreamplayer.ui.StoredFilesScreen
import com.example.torrentstreamplayer.ui.PieceMap
import com.example.torrentstreamplayer.engine.PieceMapSnapshot
import com.example.torrentstreamplayer.ui.MagnetHistoryScreen
import com.example.torrentstreamplayer.ui.theme.TorrentStreamPlayerTheme
import com.example.torrentstreamplayer.ui.theme.PlaybackColors

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()
        setContent {
            TorrentStreamPlayerTheme {
                val uiState by viewModel.uiState.collectAsState()
                val completionAd = remember { com.example.torrentstreamplayer.ui.CompletionInterstitial(this) }
                DisposableEffect(completionAd) { onDispose { completionAd.dispose() } }
                LaunchedEffect(uiState is StreamUiState.Playing) {
                    if (uiState is StreamUiState.Playing) {
                        try {
                            com.example.torrentstreamplayer.ui.TestAdsRuntime.initialize(this@MainActivity)
                            completionAd.preload()
                        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                        catch (failure: Exception) { android.util.Log.w("CompletionAd", "Load failed", failure) }
                    }
                }
                val pieces by if (uiState is StreamUiState.Buffering || uiState is StreamUiState.FetchingMetadata)
                    viewModel.torrentEngine.pieceMap.collectAsState()
                else remember { mutableStateOf(PieceMapSnapshot.EMPTY) }
                var showFiles by remember { mutableStateOf(false) }
                var showHistory by remember { mutableStateOf(false) }
                var showDownloadDialog by remember { mutableStateOf(false) }
                var simplePlaylist by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
                var playlistSort by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(com.example.torrentstreamplayer.ui.PlaylistSort.RECENT_PLAY) }
                var playlistFilter by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(com.example.torrentstreamplayer.ui.PlaylistFilter.ALL) }
                var returnToHistory by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
                fun closePlayback() {
                    if (returnToHistory) viewModel.stopStream() else viewModel.stopForMain()
                    showHistory = returnToHistory
                    returnToHistory = false
                }
                val downloadJobs by viewModel.downloads.tasks.collectAsState()
                if (showDownloadDialog) com.example.torrentstreamplayer.ui.QuickDownloadDialog(
                    onDismiss = { showDownloadDialog = false }, submit = viewModel::queueDownload)
                val downloadError by viewModel.downloads.error.collectAsState()
                val metadata by if (uiState is StreamUiState.FetchingMetadata || uiState is StreamUiState.Buffering)
                    viewModel.torrentEngine.metadataDetails.collectAsState()
                else remember { mutableStateOf(com.example.torrentstreamplayer.engine.MetadataDetails()) }
                androidx.activity.compose.BackHandler(enabled = com.example.torrentstreamplayer.ui.AppBackPolicy.returnsToInput(uiState, showFiles, showHistory)) {
                    closePlayback()
                }

                Scaffold(modifier = Modifier.fillMaxSize(), bottomBar = {
                    if (com.example.torrentstreamplayer.ui.TestAdPolicy.visible(uiState, showFiles, showHistory))
                        Box(Modifier.navigationBarsPadding()) {
                            com.example.torrentstreamplayer.ui.TestBannerAd()
                        }
                }) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (showFiles) {
                            StoredFilesScreen(viewModel.torrentEngine::storedFiles,
                                viewModel::deleteStoredFile, onBack = { showFiles = false })
                        } else if (showHistory) {
                            MagnetHistoryScreen(viewModel::savedMagnets, viewModel::deleteSavedMagnet,
                                onPlay = { returnToHistory = true; showHistory = false; viewModel.startStreaming(it) },
                                onBack = { showHistory = false }, onDownload = { viewModel.startStreaming(it, downloadOnly = true) },
                                onStopDownload = viewModel::stopDownloads, downloadJobs = downloadJobs,
                                onStopItem = viewModel::stopDownload, onStopItems = viewModel::stopDownloads,
                                downloadError = downloadError,
                                simple = simplePlaylist, onSimpleChange = { simplePlaylist = it },
                                downloadCount = downloadJobs.values.count { it.phase == com.example.torrentstreamplayer.ui.DownloadPhase.RUNNING },
                                sort = playlistSort, onSortChange = { playlistSort = it },
                                filter = playlistFilter, onFilterChange = { playlistFilter = it },
                                onAddDownload = { showDownloadDialog = true }, onResetPosition = viewModel::resetPlaybackPosition)
                        } else when (val state = uiState) {
                            is StreamUiState.Idle -> {
                                MagnetLinkInputScreen(
                                    onStartStream = { returnToHistory = false; viewModel.startStreaming(it) },
                                    errorMessage = null,
                                    onOpenFiles = { viewModel.stopStream(); showFiles = true },
                                    onOpenHistory = { showHistory = true }
                                )
                            }

                            is StreamUiState.FetchingMetadata -> {
                                LoadingScreen(
                                    title = if (metadata.hasMetadata) "토렌트 정보 수신 완료" else "토렌트 정보 가져오는 중…",
                                    subtitle = "파일 정보와 연결 상태를 확인합니다.",
                                    details = { com.example.torrentstreamplayer.ui.MetadataDetailsPanel(metadata, pieces) },
                                    onCancel = { returnToHistory = false; closePlayback() },
                                    onOpenPlaylist = { viewModel.continueInPlaylist(); returnToHistory = false; showHistory = true }
                                )
                            }

                            is StreamUiState.Buffering -> {
                                BufferingScreen(
                                    state = state,
                                    pieces = pieces,
                                    details = { com.example.torrentstreamplayer.ui.MetadataDetailsPanel(metadata, pieces) },
                                    onCancel = { returnToHistory = false; closePlayback() },
                                    onOpenPlaylist = { viewModel.continueInPlaylist(); returnToHistory = false; showHistory = true }
                                )
                            }

                            is StreamUiState.Playing -> {
                                val positionWriter = remember(viewModel.torrentEngine) { viewModel.playbackPositionWriter() }
                                VlcPlayerScreen(
                                    fileName = state.fileName,
                                    torrentEngine = viewModel.torrentEngine,
                                    onBackClicked = { closePlayback() },
                                    onAddDownload = { showDownloadDialog = true },
                                    initialPositionMs = viewModel.resumePositionMs, onPositionChanged = positionWriter,
                                    onPlaybackCompleted = { watchedMs ->
                                        viewModel.stopStream(); returnToHistory = false; showHistory = true
                                        completionAd.showIfReady(watchedMs,
                                            lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED))
                                    }
                                )
                            }

                            is StreamUiState.Error -> {
                                MagnetLinkInputScreen(
                                    onStartStream = { returnToHistory = false; viewModel.startStreaming(it) },
                                    errorMessage = state.message,
                                    onOpenFiles = { viewModel.stopStream(); showFiles = true },
                                    onOpenHistory = { viewModel.stopStream(); showHistory = true }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MagnetLinkInputScreen(
    onStartStream: (String) -> Unit,
    errorMessage: String?,
    onOpenFiles: () -> Unit = {},
    onOpenHistory: () -> Unit = {}
) {
    var magnetLink by remember { mutableStateOf("") }
    val context = androidx.compose.ui.platform.LocalContext.current
    val versionName = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(R.drawable.pieceplay_icon_foreground),
            contentDescription = "PiecePlay 로고",
            modifier = Modifier.padding(bottom = 12.dp).size(96.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
        )
        Text(
            text = "PiecePlay",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))

        Text(versionName, color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall)

        OutlinedTextField(
            value = magnetLink,
            onValueChange = { magnetLink = it },
            label = { Text("마그넷 또는 .torrent 다운로드 링크") },
            placeholder = { Text("magnet:?xt=urn:btih:...") },
            isError = errorMessage != null,
            modifier = Modifier.fillMaxWidth()
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { onStartStream(magnetLink) },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Stream & Auto Play")
        }
        TextButton(onClick = onOpenHistory, modifier = Modifier.padding(top = 12.dp)) {
            Text("재생 목록")
        }
        TextButton(onClick = onOpenFiles) { Text("저장 파일 관리") }
    }
}

@Composable
fun LoadingScreen(
    title: String,
    subtitle: String,
    details: @Composable () -> Unit = {},
    onCancel: () -> Unit,
    onOpenPlaylist: () -> Unit = onCancel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        details()
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        com.example.torrentstreamplayer.ui.PreparationActions(onCancel, onOpenPlaylist)
    }
}

@Composable
fun BufferingScreen(
    state: StreamUiState.Buffering,
    onCancel: () -> Unit,
    pieces: PieceMapSnapshot = PieceMapSnapshot.EMPTY,
    details: (@Composable () -> Unit)? = null,
    onOpenPlaylist: () -> Unit = onCancel
) {
    val speedKb = state.downloadSpeedBytes / 1024
    val speedMb = speedKb / 1024.0
    val speedText = if (speedMb >= 1.0) {
        String.format("%.2f MB/s", speedMb)
    } else {
        "$speedKb KB/s"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = state.fileName,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (details != null) details() else PieceMap(pieces, Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = (if (state.autoPlay) "Buffering: " else "다운로드: ") + "${state.progress.toInt()}%",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = speedText,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        com.example.torrentstreamplayer.ui.PreparationActions(onCancel, onOpenPlaylist)
    }
}
