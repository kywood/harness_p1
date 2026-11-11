package com.example.torrentstreamplayer.ui

import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch

@Composable
internal fun QuickDownloadDialog(onDismiss: () -> Unit, submit: suspend (String) -> Unit) {
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("다운로드 추가") },
        text = {
            androidx.compose.foundation.layout.Column {
                OutlinedTextField(value = input, onValueChange = { input = it; error = null },
                    enabled = !busy, label = { Text("마그넷 또는 .torrent 파일 링크") }, maxLines = 3,
                    isError = error != null)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (busy) LinearProgressIndicator()
            }
        },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("취소") } },
        confirmButton = {
            TextButton(enabled = !busy && input.isNotBlank(), onClick = {
                busy = true
                scope.launch {
                    try { submit(input); onDismiss() }
                    catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                    catch (failure: Exception) { error = failure.localizedMessage ?: "주소를 확인해 주세요." }
                    finally { busy = false }
                }
            }) { Text("다운로드") }
        })
}
