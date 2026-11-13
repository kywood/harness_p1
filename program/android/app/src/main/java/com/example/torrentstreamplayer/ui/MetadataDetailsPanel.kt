package com.example.torrentstreamplayer.ui

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.torrentstreamplayer.R
import com.example.torrentstreamplayer.engine.MetadataDetails
import com.example.torrentstreamplayer.ui.theme.Spacing

/** 준비 화면의 접는 세부 정보: 진행 단계, 정보 출처, 파일·피스 구성, 받은 양. */
@Composable
internal fun MetadataDetailsPanel(details: MetadataDetails) {
    val context = LocalContext.current
    fun size(bytes: Long) = Formatter.formatFileSize(context, bytes)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(stringResource(details.stage.label, details.elapsedSec), style = MaterialTheme.typography.titleSmall)
        details.origin?.let { DetailLine(stringResource(R.string.metadata_source, stringResource(it.label), details.peers)) }
        if (details.hasMetadata) {
            Text(details.name.orEmpty(), style = MaterialTheme.typography.titleMedium, maxLines = 2)
            DetailLine(stringResource(R.string.metadata_files, details.fileCount, size(details.totalSize)))
            DetailLine(stringResource(R.string.metadata_pieces, details.pieceCount, size(details.pieceSize)))
            DetailLine(
                if (details.videoSize > 0) {
                    stringResource(R.string.metadata_received_video, size(details.downloadedBytes), size(details.videoSize))
                } else {
                    stringResource(R.string.metadata_received_video_pending, size(details.downloadedBytes))
                }
            )
        } else {
            DetailLine(stringResource(R.string.metadata_unknown))
        }
    }
}

@Composable
private fun DetailLine(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
