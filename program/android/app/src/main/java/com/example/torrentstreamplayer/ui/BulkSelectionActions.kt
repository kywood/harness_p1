package com.example.torrentstreamplayer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import com.example.torrentstreamplayer.R
import com.example.torrentstreamplayer.ui.components.ConfirmDialog
import com.example.torrentstreamplayer.ui.components.TextAction
import com.example.torrentstreamplayer.ui.components.TextActionTone
import com.example.torrentstreamplayer.ui.theme.Shapes
import com.example.torrentstreamplayer.ui.theme.Spacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 일괄 선택 줄: 전체 선택 · 삭제 · [trailingActions](다운로드/중지)를 한 줄에 둔다.
 * 하나라도 고르면 줄이 `surfaceContainer` 막대로 바뀌어 선택 모드임을 보여 주고, 삭제와 뒤 동작이 나타난다.
 */
@Composable
internal fun BulkSelectionActions(keys: List<String>, selected: Set<String>,
    onSelection: (Set<String>) -> Unit, delete: suspend (String) -> Boolean, description: String,
    enabled: Boolean, onBusy: (Boolean) -> Unit, onChanged: () -> Unit,
    deleteLabel: String? = null, trailingActions: @Composable (Boolean) -> Unit = {}) {
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current
    var pending by remember { mutableStateOf<Set<String>?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val state = when {
        selected.isEmpty() -> ToggleableState.Off
        keys.isNotEmpty() && selected.containsAll(keys) -> ToggleableState.On
        else -> ToggleableState.Indeterminate
    }
    val selecting = selected.isNotEmpty()
    val colors = MaterialTheme.colorScheme
    Column {
        Row(
            Modifier.fillMaxWidth()
                .background(if (selecting) colors.surfaceContainer else Color.Transparent, Shapes.small)
                .padding(end = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TriStateCheckbox(state, enabled = enabled && !busy && keys.isNotEmpty(),
                onClick = { onSelection(BulkSelection.all(keys, selected)) },
                colors = CheckboxDefaults.colors(checkedColor = colors.tertiary, checkmarkColor = colors.background,
                    uncheckedColor = colors.onSurfaceVariant))
            TextAction(stringResource(R.string.bulk_select_all), { onSelection(BulkSelection.all(keys, selected)) },
                enabled = enabled && !busy && keys.isNotEmpty(), tone = TextActionTone.MUTED)
            Spacer(Modifier.weight(1f))
            if (selecting) {
                TextAction(deleteLabel ?: stringResource(R.string.bulk_delete_selected, selected.size),
                    { pending = selected.toSet(); error = null }, enabled = enabled && !busy,
                    tone = TextActionTone.DESTRUCTIVE)
                trailingActions(enabled && !busy)
            }
        }
        error?.let { Text(it, color = colors.error, style = MaterialTheme.typography.bodySmall) }
    }
    pending?.let { targets ->
        ConfirmDialog(
            title = pluralStringResource(R.plurals.bulk_delete_title, targets.size, targets.size),
            message = description,
            confirmLabel = stringResource(if (busy) R.string.action_deleting else R.string.action_delete),
            dismissLabel = stringResource(R.string.action_cancel),
            onConfirm = {
                busy = true
                onBusy(true)
                scope.launch {
                    try {
                        val removed = withContext(Dispatchers.IO) { BulkSelection.delete(targets) { delete(it) } }
                        onSelection(selected - removed)
                        if (removed.size < targets.size) error = (targets.size - removed.size).let { resources.getQuantityString(R.plurals.bulk_delete_partial, it, it) }
                        pending = null
                        onChanged()
                    } finally {
                        busy = false
                        onBusy(false)
                    }
                }
            },
            onDismiss = { pending = null },
            enabled = !busy
        )
    }
}
