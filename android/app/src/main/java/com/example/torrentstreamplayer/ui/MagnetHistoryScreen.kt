package com.example.torrentstreamplayer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.torrentstreamplayer.engine.SavedMagnet
import com.example.torrentstreamplayer.engine.MagnetIdentity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MagnetHistoryScreen(load: () -> List<SavedMagnet>, delete: (String) -> Unit,
    onPlay: (String) -> Unit, onBack: () -> Unit, onDownload: (String) -> Unit = {},
    onStopDownload: () -> Unit = {}, activeKey: String? = null,
    activeProgress: Pair<Long, Long>? = null,
    activePieces: com.example.torrentstreamplayer.engine.PieceMapSnapshot = com.example.torrentstreamplayer.engine.PieceMapSnapshot.EMPTY,
    downloadError: String? = null, downloadSpeedBytes: Long = 0) {
    val scope = rememberCoroutineScope()
    var selected by remember { mutableStateOf(emptySet<String>()) }
    var bulkBusy by remember { mutableStateOf(false) }
    var entries by remember { mutableStateOf(emptyList<SavedMagnet>()) }
    var refresh by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    var deleting by remember { mutableStateOf<SavedMagnet?>(null) }
    BackHandler { if (!bulkBusy) onBack() }
    LaunchedEffect(entries) { selected = selected.intersect(entries.map { it.magnet }.toSet()) }
    LaunchedEffect(refresh, activeKey) {
        try { do {
            entries = withContext(Dispatchers.IO) { load() }
            if (activeKey != null) kotlinx.coroutines.delay(2000)
        } while (activeKey != null) }
        catch (_: Exception) { error = "목록을 읽지 못했습니다." }
    }
    Scaffold(topBar = {
        TopAppBar(title = { Text("재생 목록") },
            navigationIcon = { TextButton(onClick = onBack, enabled = !bulkBusy) { Text("닫기") } })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("최근 50개를 암호화해 보관합니다.", style = MaterialTheme.typography.bodySmall)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            downloadError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (activeKey != null) TextButton(onClick = onStopDownload) { Text("다운로드 중지") }
            if (entries.isEmpty()) Text("재생 목록이 비어 있습니다.")
            BulkSelectionActions(entries.map { it.magnet }, selected, { selected = it },
                { delete(it); true }, "선택한 항목의 다운로드를 중지하고 영상·메타데이터·이어받기 기록을 함께 삭제합니다. 복구할 수 없습니다.",
                deleting == null, { bulkBusy = it }, { refresh++ })
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(entries, key = { MagnetIdentity.key(it.magnet) }) { entry ->
                    val active = activeKey == MagnetIdentity.key(entry.magnet)
                    val shown = if (active && activeProgress != null) entry.copy(fileSize = activeProgress.first,
                        downloadedBytes = activeProgress.second, pieces = activePieces) else entry
                    val complete = shown.fileSize?.let { it > 0 && it == shown.downloadedBytes } == true
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Text(entry.title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                            Checkbox(checked = entry.magnet in selected, enabled = !bulkBusy && deleting == null,
                                onCheckedChange = { selected = BulkSelection.toggle(selected, entry.magnet, it) })
                            }
                            Text("최근 요청: " + java.time.format.DateTimeFormatter
                                .ofPattern("yyyy-MM-dd HH:mm:ss").withZone(java.time.ZoneId.systemDefault())
                                .format(java.time.Instant.ofEpochMilli(entry.addedAt)),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(entry.magnet, maxLines = 2, style = MaterialTheme.typography.bodySmall)
                            SavedMagnetProgress(shown, if (active && !complete) downloadSpeedBytes else null)
                            PieceMap(shown.pieces ?: com.example.torrentstreamplayer.engine.PieceMapSnapshot.EMPTY, compact = true)
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                TextButton(onClick = { onPlay(entry.magnet) }, enabled = !bulkBusy) { Text("재생") }
                                TextButton(onClick = { if (active) onStopDownload() else onDownload(entry.magnet) },
                                    enabled = !bulkBusy && !complete) { Text(if (complete) "완료" else if (active) "중지" else "다운로드") }
                                TextButton(onClick = { deleting = entry }, enabled = !bulkBusy) { Text("파일과 함께 삭제") }
                            }
                        }
                    }
                }
            }
        }
    }
    deleting?.let { entry ->
        AlertDialog(onDismissRequest = { if (!bulkBusy) deleting = null },
            title = { Text("영상 파일과 함께 삭제할까요?") },
            text = { Text("이 항목의 다운로드를 중지하고 영상·메타데이터·이어받기 기록을 모두 삭제합니다. 복구할 수 없습니다.") },
            dismissButton = { TextButton(enabled = !bulkBusy, onClick = { deleting = null }) { Text("취소") } },
            confirmButton = {
                TextButton(enabled = !bulkBusy, onClick = {
                    bulkBusy = true
                    scope.launch {
                        try { withContext(Dispatchers.IO) { delete(entry.magnet) }; refresh++ }
                        catch (e: Exception) { error = e.localizedMessage ?: "관련 파일을 삭제하지 못했습니다." }
                        finally { bulkBusy = false }
                        deleting = null
                    }
                }) { Text("삭제") }
            })
    }
}
