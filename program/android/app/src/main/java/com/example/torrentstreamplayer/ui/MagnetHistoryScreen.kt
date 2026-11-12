package com.example.torrentstreamplayer.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import com.example.torrentstreamplayer.R
import com.example.torrentstreamplayer.engine.MagnetIdentity
import com.example.torrentstreamplayer.engine.PieceMapSnapshot
import com.example.torrentstreamplayer.engine.SavedMagnet
import com.example.torrentstreamplayer.ui.components.AppTopBar
import com.example.torrentstreamplayer.ui.components.ConfirmDialog
import com.example.torrentstreamplayer.ui.components.EmptyState
import com.example.torrentstreamplayer.ui.components.ProgressBar
import com.example.torrentstreamplayer.ui.components.SecondaryButton
import com.example.torrentstreamplayer.ui.components.SectionCard
import com.example.torrentstreamplayer.ui.components.StatusChip
import com.example.torrentstreamplayer.ui.components.TextAction
import com.example.torrentstreamplayer.ui.components.TextActionTone
import com.example.torrentstreamplayer.ui.theme.Sizes
import com.example.torrentstreamplayer.ui.theme.Spacing
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** 다운로드가 진행 중일 때 목록을 다시 읽는 주기. */
private const val REFRESH_INTERVAL_MS = 2000L

/** 받은 날짜 표시 형식(숫자 형식이라 번역하지 않는다). */
private val ADDED_AT_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault())

@Composable
internal fun MagnetHistoryScreen(load: () -> List<SavedMagnet>, delete: suspend (String) -> Unit,
    onPlay: (String) -> Unit, onBack: () -> Unit, onDownload: (String) -> Unit = {},
    onStopDownload: () -> Unit = {}, activeKey: String? = null,
    activeProgress: Pair<Long, Long>? = null,
    activePieces: PieceMapSnapshot = PieceMapSnapshot.EMPTY,
    downloadError: String? = null, downloadSpeedBytes: Long = 0,
    simple: Boolean = false, onSimpleChange: (Boolean) -> Unit = {}, downloadCount: Int = 0,
    sort: PlaylistSort = PlaylistSort.RECENT_PLAY, onSortChange: (PlaylistSort) -> Unit = {},
    filter: PlaylistFilter = PlaylistFilter.ALL, onFilterChange: (PlaylistFilter) -> Unit = {},
    downloadJobs: Map<String, DownloadEntry> = emptyMap(), onStopItem: (String) -> Unit = {},
    onStopItems: ((Set<String>) -> Unit)? = null, onAddDownload: (() -> Unit)? = null,
    onResetPosition: (suspend (String) -> Unit)? = null, waitsForWifi: Boolean = false) {
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current
    var selected by remember { mutableStateOf(emptySet<String>()) }
    var bulkBusy by remember { mutableStateOf(false) }
    var entries by remember { mutableStateOf(emptyList<SavedMagnet>()) }
    var refresh by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var deleting by remember { mutableStateOf<SavedMagnet?>(null) }
    val liveEntries = entries.map { entry ->
        val task = downloadJobs[MagnetIdentity.key(entry.magnet)]
        val snapshot = task?.takeIf { it.phase.stoppable }?.snapshot
        if (snapshot != null) entry.copy(fileSize = snapshot.size, downloadedBytes = snapshot.downloaded, pieces = snapshot.pieces)
        else
        if (MagnetIdentity.key(entry.magnet) == activeKey && activeProgress != null)
            entry.copy(fileSize = activeProgress.first, downloadedBytes = activeProgress.second, pieces = activePieces) else entry
    }
    val visibleEntries = PlaylistOrdering.visible(liveEntries, sort, filter)
    val selectedKeys = visibleEntries.filter { it.magnet in selected }.map { MagnetIdentity.key(it.magnet) }.toSet()
    val stoppable = DownloadSelection.stoppable(selectedKeys, downloadJobs, activeKey)
    val targets = PlaylistOrdering.downloadTargets(visibleEntries, selected)
    val summary = PlaylistSummary.of(downloadJobs.values, downloadCount)
    val refreshing = activeKey != null || summary.hasLiveJobs
    val jobIdentity = downloadJobs.values.joinToString { it.key + it.phase.name }
    val loadErrorText = stringResource(R.string.playlist_load_error)
    val snackbar = remember { SnackbarHostState() }
    val waitingForWifiText = stringResource(R.string.info_waiting_for_wifi)
    // 다운로드 버튼: Wi-Fi 전용 + 모바일 데이터면 작업은 바로 Wi-Fi 대기로 들어가고 그 사실을 스낵바로 알린다.
    val startDownloads: (List<String>) -> Unit = { magnets ->
        magnets.forEach(onDownload)
        if (magnets.isNotEmpty() && waitsForWifi) scope.launch { snackbar.showSnackbar(waitingForWifiText) }
    }
    BackHandler { if (!bulkBusy) onBack() }
    LaunchedEffect(visibleEntries) { selected = selected.intersect(visibleEntries.map { it.magnet }.toSet()) }
    LaunchedEffect(refresh, activeKey, jobIdentity) {
        try { do {
            entries = withContext(Dispatchers.IO) { load() }
            loadError = null
            if (refreshing) delay(REFRESH_INTERVAL_MS)
        } while (refreshing) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { loadError = loadErrorText }
    }
    Column(Modifier.fillMaxSize()) {
        AppTopBar(stringResource(R.string.action_open_playlist), onBack = onBack,
            backLabel = stringResource(R.string.action_close), backEnabled = !bulkBusy) {
            onAddDownload?.let { TextAction(stringResource(R.string.action_add_link), it, enabled = !bulkBusy) }
            TextAction(stringResource(if (simple) R.string.action_detailed else R.string.action_simple),
                { onSimpleChange(!simple) }, enabled = !bulkBusy)
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                Modifier.widthIn(max = Sizes.contentMaxWidth).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = Spacing.screenGutter, vertical = Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                item(key = "summary") {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(summaryText(summary), Modifier.weight(1f), color = MaterialTheme.colorScheme.tertiary,
                                style = MaterialTheme.typography.titleSmall)
                            if (refreshing) TextAction(stringResource(if (downloadJobs.isEmpty()) R.string.action_stop_download
                                else R.string.action_stop_all), onStopDownload, enabled = !bulkBusy,
                                tone = TextActionTone.DESTRUCTIVE)
                        }
                        Text(stringResource(R.string.playlist_storage_note), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                item(key = "options") { PlaylistOptions(sort, onSortChange, filter, onFilterChange, !bulkBusy) }
                if (loadError != null || error != null || downloadError != null) item(key = "errors") {
                    Column {
                        loadError?.let { message -> Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(message, Modifier.weight(1f), color = MaterialTheme.colorScheme.error)
                            TextAction(stringResource(R.string.action_retry), { refresh++ }, enabled = !bulkBusy)
                        } }
                        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        downloadError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    }
                }
                if (visibleEntries.isNotEmpty()) item(key = "selection") {
                    Box {
                        BulkSelectionActions(visibleEntries.map { it.magnet }, selected, { selected = it },
                            { delete(it); true }, stringResource(R.string.playlist_bulk_delete_message),
                            deleting == null, { bulkBusy = it }, { refresh++ },
                            deleteLabel = stringResource(R.string.action_delete)) { enabled ->
                            TextAction(stringResource(if (stoppable.isEmpty()) R.string.action_download else R.string.action_stop),
                                enabled = enabled && !bulkBusy && (stoppable.isNotEmpty() || targets.isNotEmpty()), onClick = {
                                if (stoppable.isEmpty()) startDownloads(targets.map { it.magnet })
                                else {
                                    val keys = stoppable.filter { it in downloadJobs }.toSet()
                                    if (onStopItems != null) onStopItems(keys) else keys.forEach(onStopItem)
                                    if (activeKey in stoppable && activeKey !in downloadJobs) onStopDownload()
                                }
                            })
                        }
                    }
                }
                if (visibleEntries.isEmpty()) item(key = "empty") {
                    if (entries.isEmpty()) EmptyState(Icons.AutoMirrored.Filled.List, stringResource(R.string.playlist_empty),
                        actionLabel = onAddDownload?.let { stringResource(R.string.action_add_link) }, onAction = onAddDownload)
                    else EmptyState(Icons.Filled.Info, stringResource(R.string.playlist_filter_empty))
                }
                items(visibleEntries, key = { MagnetIdentity.key(it.magnet) }) { entry ->
                    val key = MagnetIdentity.key(entry.magnet)
                    val task = downloadJobs[key]
                    val active = task?.phase == DownloadPhase.RUNNING || activeKey == key
                    val queued = task?.phase == DownloadPhase.QUEUED
                    val status = PlaylistItemStatus.of(active, queued, task?.error != null, PlaylistOrdering.complete(entry),
                        waitingForWifi = task?.phase == DownloadPhase.WAITING_FOR_WIFI)
                    PlaylistItemCard(
                        entry = entry,
                        active = active,
                        status = status,
                        speedBytes = if (active && status.showsSpeed && !PlaylistOrdering.complete(entry))
                            task?.speed ?: downloadSpeedBytes else null,
                        errorMessage = task?.error,
                        simple = simple,
                        checked = entry.magnet in selected,
                        checkEnabled = !bulkBusy && deleting == null,
                        actionsEnabled = !bulkBusy,
                        onCheckedChange = { selected = BulkSelection.toggle(selected, entry.magnet, it) },
                        onPlay = { onPlay(entry.magnet) },
                        onDownloadAction = {
                            if ((active || task?.phase?.stoppable == true) && task != null) onStopItem(key)
                            else if (active) onStopDownload() else startDownloads(listOf(entry.magnet))
                        },
                        onDelete = { deleting = entry },
                        onResetPosition = onResetPosition?.let { reset -> {
                            scope.launch {
                                try {
                                    reset(entry.magnet)
                                    refresh++
                                } catch (cancelled: CancellationException) {
                                    throw cancelled
                                } catch (failure: Exception) {
                                    error = failure.localizedMessage ?: resources.getString(R.string.playlist_reset_failed)
                                }
                            }
                        } }
                    )
                }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
        }
    }
    deleting?.let { entry ->
        ConfirmDialog(
            title = stringResource(R.string.playlist_delete_title),
            message = stringResource(R.string.playlist_delete_message),
            confirmLabel = stringResource(if (bulkBusy) R.string.action_deleting else R.string.action_delete),
            dismissLabel = stringResource(R.string.action_cancel),
            onConfirm = {
                bulkBusy = true
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) { delete(entry.magnet) }
                        refresh++
                    } catch (e: Exception) {
                        error = e.localizedMessage ?: resources.getString(R.string.playlist_delete_failed)
                    } finally {
                        bulkBusy = false
                    }
                    deleting = null
                }
            },
            onDismiss = { deleting = null },
            enabled = !bulkBusy
        )
    }
}

/** 요약 줄: 다운로드 중(늘) · 대기(있을 때) · Wi-Fi 대기(있을 때). */
@Composable
private fun summaryText(summary: PlaylistSummary): String {
    val base = if (summary.showsQueued)
        stringResource(R.string.playlist_summary_queued, summary.downloading, summary.queued)
        else stringResource(R.string.playlist_summary, summary.downloading)
    if (!summary.showsWifiWaiting) return base
    val wifi = stringResource(R.string.playlist_summary_wifi_waiting, summary.waitingForWifi)
    return listOf(base, wifi).joinToString(" ${stringResource(R.string.separator_dot)} ")
}

/** 항목 카드의 다운로드 버튼 모양: 상태마다 문구와 아이콘. */
private enum class DownloadButton(@StringRes val label: Int) {
    CANCEL_QUEUE(R.string.action_cancel_queue),
    STOP(R.string.action_stop),
    COMPLETE(R.string.label_complete),
    START(R.string.action_download);

    val icon: ImageVector
        @Composable get() = when (this) {
            CANCEL_QUEUE -> Icons.Filled.Close
            STOP -> ImageVector.vectorResource(R.drawable.ic_stop)
            COMPLETE -> Icons.Filled.Check
            START -> ImageVector.vectorResource(R.drawable.ic_download)
        }

    companion object {
        fun of(status: PlaylistItemStatus, active: Boolean): DownloadButton = when {
            status == PlaylistItemStatus.QUEUED -> CANCEL_QUEUE
            status == PlaylistItemStatus.WAITING_FOR_WIFI -> STOP
            status == PlaylistItemStatus.COMPLETED -> COMPLETE
            active -> STOP
            else -> START
        }
    }
}

/** 재생 목록 항목 카드: 제목, 상태 칩·받은 양, 진행 막대, (자세히) 조각 맵·주소, 이어보기, 동작. */
@Composable
private fun PlaylistItemCard(
    entry: SavedMagnet,
    active: Boolean,
    status: PlaylistItemStatus,
    speedBytes: Long?,
    errorMessage: String?,
    simple: Boolean,
    checked: Boolean,
    checkEnabled: Boolean,
    actionsEnabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onPlay: () -> Unit,
    onDownloadAction: () -> Unit,
    onDelete: () -> Unit,
    onResetPosition: (() -> Unit)?
) {
    val colors = MaterialTheme.colorScheme
    val button = DownloadButton.of(status, active)
    SectionCard {
        Row(verticalAlignment = Alignment.Top) {
            Text(entry.title, Modifier.weight(1f).padding(top = Spacing.md), style = MaterialTheme.typography.titleMedium,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            Checkbox(checked = checked, enabled = checkEnabled, onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(checkedColor = colors.tertiary, checkmarkColor = colors.background,
                    uncheckedColor = colors.onSurfaceVariant))
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            StatusChip(stringResource(status.label), status.tone,
                icon = status.icon?.let { ImageVector.vectorResource(it) })
            SavedMagnetProgress(entry, Modifier.weight(1f), speedBytes)
        }
        val size = entry.fileSize
        ProgressBar(if (size != null && size > 0) (entry.downloadedBytes ?: 0L).toFloat() / size else 0f)
        if (!simple) {
            PieceMap(entry.pieces ?: PieceMapSnapshot.EMPTY, compact = true, labelColor = colors.onSurfaceVariant)
            Text(stringResource(R.string.playlist_added_at, ADDED_AT_FORMAT.format(Instant.ofEpochMilli(entry.addedAt))),
                style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
            Text(entry.magnet, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (entry.lastPositionMs > 0) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.playlist_resume, formatTime(entry.lastPositionMs)), Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge, color = colors.tertiary)
            if (onResetPosition != null) TextAction(stringResource(R.string.action_reset_position), onResetPosition,
                enabled = actionsEnabled, tone = TextActionTone.MUTED)
        }
        errorMessage?.let { Text(it, color = colors.error, style = MaterialTheme.typography.bodySmall) }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            SecondaryButton(stringResource(R.string.action_play), onPlay, enabled = actionsEnabled,
                icon = Icons.Filled.PlayArrow)
            TextAction(stringResource(button.label), onDownloadAction, icon = button.icon,
                enabled = actionsEnabled && (button != DownloadButton.COMPLETE || active))
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onDelete, enabled = actionsEnabled) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete_with_file),
                    tint = if (actionsEnabled) colors.onSurfaceVariant else colors.outline, modifier = Modifier.size(Sizes.icon))
            }
        }
    }
}
