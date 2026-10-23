package com.example.torrentstreamplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
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
        enableEdgeToEdge()
        setContent {
            TorrentStreamPlayerTheme {
                val uiState by viewModel.uiState.collectAsState()
                val pieces by viewModel.torrentEngine.pieceMap.collectAsState()
                var showFiles by remember { mutableStateOf(false) }
                var showHistory by remember { mutableStateOf(false) }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (showFiles) {
                            StoredFilesScreen(viewModel.torrentEngine::storedFiles,
                                viewModel.torrentEngine::deleteStoredFile, onBack = { showFiles = false })
                        } else if (showHistory) {
                            MagnetHistoryScreen(viewModel::savedMagnets, viewModel::deleteSavedMagnet,
                                onPlay = { showHistory = false; viewModel.startStreaming(it) },
                                onBack = { showHistory = false })
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
                                    title = "Fetching Torrent Metadata...",
                                    subtitle = "Connecting to peers and retrieving video info",
                                    onCancel = { viewModel.stopStream() }
                                )
                            }

                            is StreamUiState.Buffering -> {
                                BufferingScreen(
                                    state = state,
                                    pieces = pieces,
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
        TextButton(onClick = onOpenFiles, modifier = Modifier.padding(top = 12.dp)) {
            Text("저장 파일 관리")
        }
        TextButton(onClick = onOpenHistory) { Text("저장된 마그넷 목록") }
    }
}

@Composable
fun LoadingScreen(
    title: String,
    subtitle: String,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = title, style = MaterialTheme.typography.titleLarge)
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
    pieces: PieceMapSnapshot = PieceMapSnapshot.EMPTY
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

        PieceMap(pieces, Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Buffering: ${state.progress.toInt()}%",
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
