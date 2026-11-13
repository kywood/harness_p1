package com.example.torrentstreamplayer.ui

import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import com.example.torrentstreamplayer.R
import com.example.torrentstreamplayer.engine.StoredTorrentFile
import com.example.torrentstreamplayer.ui.components.AppTopBar
import com.example.torrentstreamplayer.ui.components.ConfirmDialog
import com.example.torrentstreamplayer.ui.components.EmptyState
import com.example.torrentstreamplayer.ui.theme.Shapes
import com.example.torrentstreamplayer.ui.theme.Sizes
import com.example.torrentstreamplayer.ui.theme.Spacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 저장 파일 관리: 받은 영상과 암호화된 진행 정보 파일을 확장자별로 보고 지운다. */
@Composable
fun StoredFilesScreen(loadFiles: () -> List<StoredTorrentFile>,
    deleteFile: suspend (String) -> Boolean, onBack: () -> Unit) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    var selected by remember { mutableStateOf(emptySet<String>()) }
    var bulkBusy by remember { mutableStateOf(false) }
    var files by remember { mutableStateOf(emptyList<StoredTorrentFile>()) }
    var extension by remember { mutableStateOf<String?>(null) }
    val visibleFiles = remember(files, extension) { StoredFileFilter.visible(files, extension) }
    val extensions = remember(files, extension) {
        (files.map { StoredFileFilter.extension(it.name) } + listOfNotNull(extension)).distinct().sorted()
    }
    var refresh by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var deleting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var pending by remember { mutableStateOf<StoredTorrentFile?>(null) }
    val idle = !loading && !deleting && !bulkBusy
    BackHandler(onBack = onBack)
    LaunchedEffect(visibleFiles) { selected = selected.intersect(visibleFiles.map { it.path }.toSet()) }
    LaunchedEffect(refresh) {
        loading = true
        error = null
        try { files = withContext(Dispatchers.IO) { loadFiles() } }
        catch (e: Exception) {
            error = InputFailureText.ownMessage(e, context) ?: resources.getString(R.string.stored_files_load_error)
        }
        finally { loading = false }
    }
    Column(Modifier.fillMaxSize()) {
        AppTopBar(stringResource(R.string.action_open_stored_files), onBack = onBack,
            backLabel = stringResource(R.string.action_close)) {
            IconButton(onClick = { refresh++ }, enabled = idle) {
                Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.action_refresh),
                    tint = if (idle) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline)
            }
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                Modifier.widthIn(max = Sizes.contentMaxWidth).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = Spacing.screenGutter, vertical = Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                item(key = "summary") {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        Text(stringResource(R.string.stored_files_summary, visibleFiles.size,
                            Formatter.formatFileSize(context, visibleFiles.sumOf { it.size })),
                            style = MaterialTheme.typography.headlineSmall)
                        Text(stringResource(R.string.stored_files_note), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    }
                }
                item(key = "extensions") {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        ExtensionChip(stringResource(R.string.filter_all), extension == null, idle) {
                            extension = null
                            selected = emptySet()
                        }
                        extensions.forEach { type ->
                            ExtensionChip(if (type.isEmpty()) stringResource(R.string.stored_files_no_extension)
                                else stringResource(R.string.stored_files_extension, type), extension == type, idle) {
                                extension = type
                                selected = emptySet()
                            }
                        }
                    }
                }
                if (loading) item(key = "loading") {
                    LinearProgressIndicator(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.tertiary,
                        trackColor = MaterialTheme.colorScheme.outline)
                }
                if (visibleFiles.isNotEmpty()) item(key = "selection") {
                    BulkSelectionActions(visibleFiles.map { it.path }, selected, { selected = it }, { deleteFile(it) },
                        stringResource(R.string.stored_files_bulk_delete_message),
                        !loading && !deleting, { bulkBusy = it }, { refresh++ })
                }
                if (!loading && visibleFiles.isEmpty()) item(key = "empty") {
                    if (files.isEmpty()) EmptyState(ImageVector.vectorResource(R.drawable.ic_folder),
                        stringResource(R.string.stored_files_empty))
                    else EmptyState(Icons.Filled.Info, stringResource(R.string.stored_files_filter_empty))
                }
                items(visibleFiles, key = { it.path }) { file ->
                    StoredFileRow(file, Formatter.formatFileSize(context, file.size), file.path in selected,
                        checkEnabled = !deleting && !bulkBusy && !loading, deleteEnabled = !deleting && !bulkBusy,
                        onCheckedChange = { selected = BulkSelection.toggle(selected, file.path, it) },
                        onDelete = { pending = file })
                }
            }
        }
    }
    pending?.let { file ->
        ConfirmDialog(
            title = stringResource(R.string.stored_files_delete_title),
            message = stringResource(R.string.stored_files_delete_message, file.name.substringAfterLast('/')),
            confirmLabel = stringResource(if (deleting) R.string.action_deleting else R.string.action_delete),
            dismissLabel = stringResource(R.string.action_cancel),
            onConfirm = {
                scope.launch {
                    deleting = true
                    try {
                        if (withContext(Dispatchers.IO) { deleteFile(file.path) }) refresh++
                        else error = resources.getString(R.string.stored_files_delete_missing)
                        pending = null
                    } catch (e: Exception) {
                        error = InputFailureText.ownMessage(e, context)
                            ?: resources.getString(R.string.stored_files_delete_failed)
                        pending = null
                    } finally {
                        deleting = false
                    }
                }
            },
            onDismiss = { pending = null },
            enabled = !deleting
        )
    }
}

@Composable
private fun ExtensionChip(label: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    FilterChip(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        label = { Text(label, style = MaterialTheme.typography.labelLarge) },
        shape = Shapes.pill,
        border = null,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = colors.surfaceContainer,
            labelColor = colors.onSurface,
            selectedContainerColor = colors.primary,
            selectedLabelColor = colors.onPrimary
        )
    )
}

/** 저장 파일 한 줄: 형식 아이콘, 이름, 크기, 선택, 삭제. */
@Composable
private fun StoredFileRow(
    file: StoredTorrentFile,
    sizeText: String,
    checked: Boolean,
    checkEnabled: Boolean,
    deleteEnabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(color = colors.surface, shape = Shapes.medium, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = Spacing.lg, end = Spacing.xs, top = Spacing.sm, bottom = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Box(Modifier.size(Sizes.iconLarge).background(colors.surfaceContainer, Shapes.small),
                contentAlignment = Alignment.Center) {
                Icon(ImageVector.vectorResource(StoredFileKind.of(file.name).icon), contentDescription = null,
                    tint = colors.tertiary, modifier = Modifier.size(Sizes.icon))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(file.name.substringAfterLast('/'), style = MaterialTheme.typography.titleSmall, maxLines = 2,
                    overflow = TextOverflow.Ellipsis)
                Text(sizeText, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                Text(file.name.substringBeforeLast('/', ""), style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Checkbox(checked = checked, enabled = checkEnabled, onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(checkedColor = colors.tertiary, checkmarkColor = colors.background,
                    uncheckedColor = colors.onSurfaceVariant))
            IconButton(onClick = onDelete, enabled = deleteEnabled) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete),
                    tint = if (deleteEnabled) colors.onSurfaceVariant else colors.outline)
            }
        }
    }
}
