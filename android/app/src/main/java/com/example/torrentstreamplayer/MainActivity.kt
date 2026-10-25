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
                val pieces by if (uiState is StreamUiState.Buffering || uiState is StreamUiState.FetchingMetadata)
                    viewModel.torrentEngine.pieceMap.collectAsState()
                else remember { mutableStateOf(PieceMapSnapshot.EMPTY) }
                var showFiles by remember { mutableStateOf(false) }
                var showHistory by remember { mutableStateOf(false) }
                val downloadTask by viewModel.downloads.state.collectAsState()
                val activeKey = downloadTask.key
                val activeDownload by if (showHistory && activeKey != null)
                    viewModel.downloads.engine.activeDownload.collectAsState()
                else remember { mutableStateOf<com.example.torrentstreamplayer.engine.DownloadSnapshot?>(null) }
                val metadata by if (uiState is StreamUiState.FetchingMetadata || uiState is StreamUiState.Buffering)
                    viewModel.torrentEngine.metadataDetails.collectAsState()
                else remember { mutableStateOf(com.example.torrentstreamplayer.engine.MetadataDetails()) }
                androidx.activity.compose.BackHandler(enabled = com.example.torrentstreamplayer.ui.AppBackPolicy.returnsToInput(uiState, showFiles, showHistory)) {
                    viewModel.stopStream()
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
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
                                onPlay = { showHistory = false; viewModel.startStreaming(it) },
                                onBack = { showHistory = false }, onDownload = { viewModel.startStreaming(it, downloadOnly = true) },
                                onStopDownload = viewModel.downloads::stop, activeKey = activeKey,
                                activeProgress = activeDownload?.let { it.size to it.downloaded },
                                activePieces = activeDownload?.pieces ?: PieceMapSnapshot.EMPTY,
                                downloadError = downloadTask.error,
                                downloadSpeedBytes = downloadTask.speed)
                        } else when (val state = uiState) {
                            is StreamUiState.Idle -> {
                                MagnetLinkInputScreen(
                                    onStartStream = { viewModel.startStreaming(it) },
                                    errorMessage = null,
                                    onOpenFiles = { viewModel.stopStream(); showFiles = true },
                                    onOpenHistory = { viewModel.stopStream(); showHistory = true }
                                )
                            }

                            is StreamUiState.FetchingMetadata -> {
                                LoadingScreen(
                                    title = if (metadata.hasMetadata) "토렌트 정보 수신 완료" else "토렌트 정보 가져오는 중…",
                                    subtitle = "파일 정보와 연결 상태를 확인합니다.",
                                    details = { com.example.torrentstreamplayer.ui.MetadataDetailsPanel(metadata, pieces) },
                                    onCancel = { viewModel.stopStream() }
                                )
                            }

                            is StreamUiState.Buffering -> {
                                BufferingScreen(
                                    state = state,
                                    pieces = pieces,
                                    details = { com.example.torrentstreamplayer.ui.MetadataDetailsPanel(metadata, pieces) },
                                    onCancel = { viewModel.stopStream() }
                                )
                            }

                            is StreamUiState.Playing -> {
                                VlcPlayerScreen(
                                    fileName = state.fileName,
                                    torrentEngine = viewModel.torrentEngine,
                                    onBackClicked = { viewModel.stopStream() }
                                )
                            }

                            is StreamUiState.Error -> {
                                MagnetLinkInputScreen(
                                    onStartStream = { viewModel.startStreaming(it) },
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Torrent Stream Player",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))

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
    onCancel: () -> Unit
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
        OutlinedButton(onClick = onCancel) {
            Text("Cancel")
        }
    }
}

@Composable
fun BufferingScreen(
    state: StreamUiState.Buffering,
    onCancel: () -> Unit,
    pieces: PieceMapSnapshot = PieceMapSnapshot.EMPTY,
    details: (@Composable () -> Unit)? = null
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

        OutlinedButton(onClick = onCancel) {
            Text("Cancel")
        }
    }
}
