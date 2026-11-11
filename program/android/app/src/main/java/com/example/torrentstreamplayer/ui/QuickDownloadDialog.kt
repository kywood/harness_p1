package com.example.torrentstreamplayer.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.torrentstreamplayer.R
import com.example.torrentstreamplayer.ui.components.ConfirmDialog
import com.example.torrentstreamplayer.ui.components.LinkTextField
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** 링크 추가: 링크를 받아 재생 없이 다운로드 작업으로 넣는다. */
@Composable
internal fun QuickDownloadDialog(onDismiss: () -> Unit, submit: suspend (String) -> Unit) {
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val invalid = stringResource(R.string.quick_download_invalid)
    ConfirmDialog(
        title = stringResource(R.string.action_add_link),
        message = null,
        confirmLabel = stringResource(R.string.action_download),
        dismissLabel = stringResource(R.string.action_cancel),
        onConfirm = {
            busy = true
            scope.launch {
                try {
                    submit(input)
                    onDismiss()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    error = failure.localizedMessage ?: invalid
                } finally {
                    busy = false
                }
            }
        },
        onDismiss = onDismiss,
        destructive = false,
        enabled = !busy,
        confirmEnabled = input.isNotBlank()
    ) {
        LinkTextField(input, {
            input = it
            error = null
        }, error, enabled = !busy, label = stringResource(R.string.label_link_input_short))
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.tertiary,
            trackColor = MaterialTheme.colorScheme.outline)
    }
}
