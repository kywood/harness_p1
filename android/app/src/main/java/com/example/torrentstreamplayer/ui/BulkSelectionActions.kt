package com.example.torrentstreamplayer.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.state.ToggleableState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun BulkSelectionActions(keys: List<String>, selected: Set<String>,
    onSelection: (Set<String>) -> Unit, delete: (String) -> Boolean, description: String,
    enabled: Boolean, onBusy: (Boolean) -> Unit, onChanged: () -> Unit) {
    val scope = rememberCoroutineScope()
    var pending by remember { mutableStateOf<Set<String>?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val state = when {
        selected.isEmpty() -> ToggleableState.Off
        keys.isNotEmpty() && selected.containsAll(keys) -> ToggleableState.On
        else -> ToggleableState.Indeterminate
    }
    Row {
        TriStateCheckbox(state, enabled = enabled && !busy && keys.isNotEmpty(),
            onClick = { onSelection(BulkSelection.all(keys, selected)) })
        TextButton(enabled = enabled && !busy && keys.isNotEmpty(),
            onClick = { onSelection(BulkSelection.all(keys, selected)) }) { Text("전체 선택") }
        TextButton(enabled = enabled && !busy && selected.isNotEmpty(),
            onClick = { pending = selected.toSet(); error = null }) { Text("선택 삭제 (${selected.size})") }
    }
    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    pending?.let { targets ->
        AlertDialog(onDismissRequest = { if (!busy) pending = null },
            title = { Text("${targets.size}개 항목을 삭제할까요?") },
            text = { Text(description) },
            dismissButton = { TextButton(enabled = !busy, onClick = { pending = null }) { Text("취소") } },
            confirmButton = {
                TextButton(enabled = !busy, onClick = {
                    busy = true
                    onBusy(true)
                    scope.launch {
                        try {
                            val removed = withContext(Dispatchers.IO) { BulkSelection.delete(targets, delete) }
                            onSelection(selected - removed)
                            if (removed.size < targets.size) error = "${targets.size - removed.size}개 삭제 실패. 실패한 항목은 선택을 유지합니다."
                            pending = null
                            onChanged()
                        } finally { busy = false; onBusy(false) }
                    }
                }) { Text(if (busy) "삭제 중…" else "삭제") }
            })
    }
}
