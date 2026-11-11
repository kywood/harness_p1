package com.example.torrentstreamplayer.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.torrentstreamplayer.ui.theme.Shapes
import com.example.torrentstreamplayer.ui.theme.TorrentStreamPlayerTheme

/**
 * 삭제처럼 되돌릴 수 없는 동작 확인. [destructive]이면 확인 버튼을 `error` 색으로 표시한다.
 * 문구는 호출하는 쪽이 `strings.xml`에서 읽어 넘긴다.
 */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = true
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = Shapes.large,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = if (destructive) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.tertiary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(dismissLabel, color = MaterialTheme.colorScheme.onSurface) }
        }
    )
}

@Preview
@Composable
private fun ConfirmDialogPreview() {
    TorrentStreamPlayerTheme {
        ConfirmDialog("파일 삭제", "삭제한 파일은 되돌릴 수 없습니다.", "삭제", "닫기", {}, {})
    }
}
