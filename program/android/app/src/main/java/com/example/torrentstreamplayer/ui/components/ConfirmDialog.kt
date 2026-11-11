package com.example.torrentstreamplayer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.torrentstreamplayer.ui.theme.Shapes
import com.example.torrentstreamplayer.ui.theme.Spacing
import com.example.torrentstreamplayer.ui.theme.TorrentStreamPlayerTheme

/**
 * 앱의 대화상자 하나의 모양: 삭제 확인, 링크 추가 등. [destructive]이면 확인 버튼을 `error` 색으로,
 * 아니면 `primaryLight`로 표시한다. 문구는 호출하는 쪽이 `strings.xml`에서 읽어 넘긴다.
 *
 * @param enabled false이면(처리 중) 두 버튼과 바깥 누르기 닫기를 모두 막는다.
 * @param confirmEnabled 확인 버튼만 따로 막는다(예: 입력이 비었을 때).
 * @param content [message] 아래에 넣을 입력창·진행 표시.
 */
@Composable
fun ConfirmDialog(
    title: String,
    message: String?,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = true,
    enabled: Boolean = true,
    confirmEnabled: Boolean = true,
    content: (@Composable ColumnScope.() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = { if (enabled) onDismiss() },
        shape = Shapes.large,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                if (message != null) Text(message, style = MaterialTheme.typography.bodyMedium)
                content?.invoke(this)
            }
        },
        confirmButton = {
            TextAction(confirmLabel, onConfirm, enabled = enabled && confirmEnabled,
                tone = if (destructive) TextActionTone.DESTRUCTIVE else TextActionTone.DEFAULT)
        },
        dismissButton = {
            TextAction(dismissLabel, onDismiss, enabled = enabled, tone = TextActionTone.MUTED)
        }
    )
}

@Preview
@Composable
private fun ConfirmDialogPreview() {
    TorrentStreamPlayerTheme {
        ConfirmDialog("파일 삭제", "삭제한 파일은 되돌릴 수 없습니다.", "삭제", "취소", {}, {})
    }
}
