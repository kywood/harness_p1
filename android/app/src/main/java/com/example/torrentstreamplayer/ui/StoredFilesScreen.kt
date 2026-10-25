package com.example.torrentstreamplayer.ui

import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.torrentstreamplayer.engine.StoredTorrentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoredFilesScreen(loadFiles: () -> List<StoredTorrentFile>,
    deleteFile: (String) -> Boolean, onBack: () -> Unit) {
    val context = LocalContext.current
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
    BackHandler(onBack = onBack)
    LaunchedEffect(visibleFiles) { selected = selected.intersect(visibleFiles.map { it.path }.toSet()) }
    LaunchedEffect(refresh) {
        loading = true
        error = null
        try { files = withContext(Dispatchers.IO) { loadFiles() } }
        catch (e: Exception) { error = e.localizedMessage ?: "파일 목록을 읽지 못했습니다." }
        finally { loading = false }
    }
    Scaffold(topBar = {
        TopAppBar(title = { Text("저장 파일") },
            navigationIcon = { TextButton(onClick = onBack) { Text("닫기") } },
            actions = { TextButton(onClick = { refresh++ }, enabled = !loading && !deleting && !bulkBusy) {
                Text("새로고침")
            } })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("다운로드 파일과 암호화된 진행 정보",
                style = MaterialTheme.typography.bodyMedium)
            Text("${visibleFiles.size}개 · " + Formatter.formatFileSize(context, visibleFiles.sumOf { it.size }),
                style = MaterialTheme.typography.headlineSmall)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = extension == null, enabled = !loading && !deleting && !bulkBusy,
                    onClick = { extension = null; selected = emptySet() }, label = { Text("전체") })
                extensions.forEach { type ->
                    FilterChip(selected = extension == type, enabled = !loading && !deleting && !bulkBusy,
                        onClick = { extension = type; selected = emptySet() },
                        label = { Text(if (type.isEmpty()) "확장자 없음" else ".$type") })
                }
            }
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (!loading && files.isEmpty()) Text("저장된 파일이 없습니다.")
            if (!loading && files.isNotEmpty() && visibleFiles.isEmpty()) Text("해당 확장자의 파일이 없습니다.")
            BulkSelectionActions(visibleFiles.map { it.path }, selected, { selected = it }, deleteFile,
                "선택한 실제 파일을 삭제합니다. 재생 중인 파일은 재생이 중단될 수 있으며 다시 받아야 할 수 있습니다.",
                !loading && !deleting, { bulkBusy = it }, { refresh++ })
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(visibleFiles, key = { it.path }) { file ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Text(file.name, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                            Checkbox(checked = file.path in selected, enabled = !deleting && !bulkBusy && !loading,
                                onCheckedChange = { selected = BulkSelection.toggle(selected, file.path, it) })
                            }
                            Text(file.path, style = MaterialTheme.typography.bodySmall)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(Formatter.formatFileSize(context, file.size))
                                TextButton(onClick = { pending = file }, enabled = !deleting && !bulkBusy) { Text("삭제") }
                            }
                        }
                    }
                }
            }
        }
    }
    pending?.let { file ->
        AlertDialog(onDismissRequest = { if (!deleting) pending = null },
            title = { Text("이 파일을 삭제할까요?") },
            text = { Text(file.path + "\n\n삭제하면 해당 파일을 다시 받아야 할 수 있습니다.") },
            dismissButton = { TextButton(onClick = { pending = null }, enabled = !deleting) { Text("취소") } },
            confirmButton = {
                TextButton(enabled = !deleting, onClick = {
                    scope.launch {
                        deleting = true
                        try {
                            check(withContext(Dispatchers.IO) { deleteFile(file.path) }) {
                                "파일이 없거나 삭제할 수 없습니다."
                            }
                            pending = null
                            refresh++
                        } catch (e: Exception) { error = e.localizedMessage ?: "삭제 실패"; pending = null }
                        finally { deleting = false }
                    }
                }) { Text(if (deleting) "삭제 중…" else "삭제") }
            })
    }
}
