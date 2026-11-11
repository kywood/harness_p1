package com.example.torrentstreamplayer.ui

import android.text.format.Formatter
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.torrentstreamplayer.engine.MetadataDetails
import com.example.torrentstreamplayer.engine.PieceMapSnapshot

@Composable
internal fun MetadataDetailsPanel(details: MetadataDetails, pieces: PieceMapSnapshot) {
    val context = LocalContext.current
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(details.phase, style = MaterialTheme.typography.titleSmall)
        if (details.source.isNotEmpty()) Text("정보 출처: ${details.source} · 연결 피어: ${details.peers}명")
        if (details.hasMetadata) {
            Text(details.name.orEmpty(), style = MaterialTheme.typography.titleMedium)
            Text("파일 ${details.fileCount}개 · " + Formatter.formatFileSize(context, details.totalSize))
            Text("전체 피스 ${details.pieceCount}개 · 피스당 " + Formatter.formatFileSize(context, details.pieceSize))
        } else Text("메타데이터 수신 전에는 파일 크기와 피스 배치를 알 수 없습니다.")
        if (pieces.total > 0) PieceMap(pieces)
        if (details.hasMetadata) Text("받은 영상: " + Formatter.formatFileSize(context, details.downloadedBytes) +
            if (details.videoSize > 0) " / " + Formatter.formatFileSize(context, details.videoSize) else " · 크기 확인 중")
        else if (details.hasMetadata) Text("피스 검증 상태를 확인하고 있습니다.")
    }
}
