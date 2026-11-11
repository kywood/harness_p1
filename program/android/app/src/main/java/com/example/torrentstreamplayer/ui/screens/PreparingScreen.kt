package com.example.torrentstreamplayer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.torrentstreamplayer.R
import com.example.torrentstreamplayer.engine.MetadataDetails
import com.example.torrentstreamplayer.engine.PieceMapSnapshot
import com.example.torrentstreamplayer.ui.MetadataDetailsPanel
import com.example.torrentstreamplayer.ui.PieceMap
import com.example.torrentstreamplayer.ui.PreparationActions
import com.example.torrentstreamplayer.ui.StreamUiState
import com.example.torrentstreamplayer.ui.navigation.AppScreen

/** 준비 화면: 토렌트 정보 가져오기([LoadingScreen])와 버퍼링([BufferingScreen]). */
@Composable
internal fun PreparingScreen(
    screen: AppScreen.Preparing,
    metadata: MetadataDetails,
    pieces: PieceMapSnapshot,
    onCancel: () -> Unit,
    onOpenPlaylist: () -> Unit
) {
    when (screen) {
        AppScreen.Preparing.FetchingMetadata -> LoadingScreen(
            title = stringResource(if (metadata.hasMetadata) R.string.title_metadata_received
                else R.string.title_metadata_fetching),
            subtitle = stringResource(R.string.subtitle_metadata_fetching),
            details = { MetadataDetailsPanel(metadata, pieces) },
            onCancel = onCancel,
            onOpenPlaylist = onOpenPlaylist
        )
        is AppScreen.Preparing.Buffering -> BufferingScreen(
            state = screen.stream,
            pieces = pieces,
            details = { MetadataDetailsPanel(metadata, pieces) },
            onCancel = onCancel,
            onOpenPlaylist = onOpenPlaylist
        )
    }
}

@Composable
internal fun LoadingScreen(
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
        PreparationActions(onCancel, onOpenPlaylist)
    }
}

@Composable
internal fun BufferingScreen(
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
                text = stringResource(if (state.autoPlay) R.string.label_buffering else R.string.label_downloading) +
                    ": ${state.progress.toInt()}%",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = speedText,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        PreparationActions(onCancel, onOpenPlaylist)
    }
}
