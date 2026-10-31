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
internal fun MagnetHistoryScreen(load: () -> List<SavedMagnet>, delete: (String) -> Unit,
    onPlay: (String) -> Unit, onBack: () -> Unit, onDownload: (String) -> Unit = {},
    onStopDownload: () -> Unit = {}, activeKey: String? = null,
    activeProgress: Pair<Long, Long>? = null,
    activePieces: com.example.torrentstreamplayer.engine.PieceMapSnapshot = com.example.torrentstreamplayer.engine.PieceMapSnapshot.EMPTY,
    downloadError: String? = null, downloadSpeedBytes: Long = 0,
    simple: Boolean = false, onSimpleChange: (Boolean) -> Unit = {}, downloadCount: Int = 0,
    sort: PlaylistSort = PlaylistSort.RECENT_PLAY, onSortChange: (PlaylistSort) -> Unit = {},
    filter: PlaylistFilter = PlaylistFilter.ALL, onFilterChange: (PlaylistFilter) -> Unit = {},
    downloadJobs: Map<String, DownloadEntry> = emptyMap(), onStopItem: (String) -> Unit = {},
    onStopItems: ((Set<String>) -> Unit)? = null) {
    val scope = rememberCoroutineScope()
    var selected by remember { mutableStateOf(emptySet<String>()) }
    var bulkBusy by remember { mutableStateOf(false) }
    var entries by remember { mutableStateOf(emptyList<SavedMagnet>()) }
    var refresh by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var deleting by remember { mutableStateOf<SavedMagnet?>(null) }
    val liveEntries = entries.map { entry ->
        val task = downloadJobs[MagnetIdentity.key(entry.magnet)]
        val snapshot = task?.takeIf { it.phase == DownloadPhase.RUNNING || it.phase == DownloadPhase.QUEUED }?.snapshot
        if (snapshot != null) entry.copy(fileSize = snapshot.size, downloadedBytes = snapshot.downloaded, pieces = snapshot.pieces)
        else
        if (MagnetIdentity.key(entry.magnet) == activeKey && activeProgress != null)
            entry.copy(fileSize = activeProgress.first, downloadedBytes = activeProgress.second, pieces = activePieces) else entry
    }
    val visibleEntries = PlaylistOrdering.visible(liveEntries, sort, filter)
    val selectedKeys = visibleEntries.filter { it.magnet in selected }.map { MagnetIdentity.key(it.magnet) }.toSet()
    val stoppable = DownloadSelection.stoppable(selectedKeys, downloadJobs, activeKey)
    val targets = PlaylistOrdering.downloadTargets(visibleEntries, selected)
    val pendingCount = downloadJobs.values.count { it.phase == DownloadPhase.QUEUED }
    val refreshing = activeKey != null || downloadCount > 0 || pendingCount > 0
    val jobIdentity = downloadJobs.values.joinToString { it.key + it.phase.name }
    BackHandler { if (!bulkBusy) onBack() }
    LaunchedEffect(visibleEntries) { selected = selected.intersect(visibleEntries.map { it.magnet }.toSet()) }
    LaunchedEffect(refresh, activeKey, jobIdentity) {
        try { do {
            entries = withContext(Dispatchers.IO) { load() }
            loadError = null
            if (refreshing) kotlinx.coroutines.delay(2000)
        } while (refreshing) }
        catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (_: Exception) { loadError = "목록을 읽지 못했습니다." }
    }
    Scaffold(topBar = {
        TopAppBar(title = { Text("재생 목록") },
            navigationIcon = { TextButton(onClick = onBack, enabled = !bulkBusy) { Text("닫기") } },
            actions = { TextButton(enabled = !bulkBusy, onClick = { onSimpleChange(!simple) }) {
                Text(if (simple) "자세히" else "간단히")
            } })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("최근 50개를 암호화해 보관합니다.", style = MaterialTheme.typography.bodySmall)
            Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("다운로드 중 ${downloadCount.coerceAtLeast(0)}개" + if (pendingCount > 0) " · 대기 ${pendingCount}개" else "", Modifier.weight(1f), color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge)
                if (refreshing) TextButton(onClick = onStopDownload, enabled = !bulkBusy) { Text(if (downloadJobs.isEmpty()) "다운로드 중지" else "전체 중지") }
            }
            PlaylistOptions(sort, onSortChange, filter, onFilterChange, !bulkBusy)
            loadError?.let { message -> Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(message, Modifier.weight(1f), color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { refresh++ }, enabled = !bulkBusy) { Text("다시 시도") }
            } }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            downloadError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (visibleEntries.isEmpty()) Text(if (entries.isEmpty()) "재생 목록이 비어 있습니다." else "해당 조건의 영상이 없습니다.")
            BulkSelectionActions(visibleEntries.map { it.magnet }, selected, { selected = it },
                { delete(it); true }, "선택한 항목의 다운로드를 중지하고 영상·메타데이터·이어받기 기록을 함께 삭제합니다. 복구할 수 없습니다.",
                deleting == null, { bulkBusy = it }, { refresh++ }, deleteLabel = "삭제") { enabled ->
                TextButton(enabled = enabled && !bulkBusy && (stoppable.isNotEmpty() || targets.isNotEmpty()), onClick = {
                    if (stoppable.isEmpty()) targets.forEach { onDownload(it.magnet) }
                    else {
                        val keys = stoppable.filter { it in downloadJobs }.toSet()
                        if (onStopItems != null) onStopItems(keys) else keys.forEach(onStopItem)
                        if (activeKey in stoppable && activeKey !in downloadJobs) onStopDownload()
                    }
                }) { Text(DownloadSelection.label(stoppable)) }
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(visibleEntries, key = { MagnetIdentity.key(it.magnet) }) { entry ->
                    val key = MagnetIdentity.key(entry.magnet)
                    val task = downloadJobs[key]
                    val active = task?.phase == DownloadPhase.RUNNING || activeKey == key
                    val queued = task?.phase == DownloadPhase.QUEUED
                    val shown = entry
                    val complete = PlaylistOrdering.complete(shown)
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Text(entry.title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                            Checkbox(checked = entry.magnet in selected, enabled = !bulkBusy && deleting == null,
                                onCheckedChange = { selected = BulkSelection.toggle(selected, entry.magnet, it) })
                            }
                            Text("받은 날짜: " + java.time.format.DateTimeFormatter
                                .ofPattern("yyyy-MM-dd HH:mm:ss").withZone(java.time.ZoneId.systemDefault())
                                .format(java.time.Instant.ofEpochMilli(entry.addedAt)),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (!simple) Text(entry.magnet, maxLines = 2, style = MaterialTheme.typography.bodySmall)
                            SavedMagnetProgress(shown, if (active && !complete) task?.speed ?: downloadSpeedBytes else null)
                            if (queued) Text("다운로드 대기 중", style = MaterialTheme.typography.labelSmall)
                            task?.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                            PieceMap(shown.pieces ?: com.example.torrentstreamplayer.engine.PieceMapSnapshot.EMPTY,
                                compact = true, showInfo = !simple)
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                TextButton(onClick = { onPlay(entry.magnet) }, enabled = !bulkBusy) { Text("재생") }
                                TextButton(onClick = {
                                    if ((active || queued) && task != null) onStopItem(key)
                                    else if (active) onStopDownload() else onDownload(entry.magnet)
                                }, enabled = !bulkBusy && (!complete || active || queued)) {
                                    Text(if (queued) "대기 취소" else if (complete) "완료" else if (active) "중지" else "다운로드")
                                }
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
