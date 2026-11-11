package com.example.torrentstreamplayer.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.torrentstreamplayer.R
import com.example.torrentstreamplayer.ui.theme.Shapes
import com.example.torrentstreamplayer.ui.theme.Sizes
import com.example.torrentstreamplayer.ui.theme.Spacing

/** 재생 목록 정렬·표시 칩. 칩을 누르면 고를 수 있는 항목이 펼쳐진다. */
@Composable
internal fun PlaylistOptions(sort: PlaylistSort, onSort: (PlaylistSort) -> Unit,
    filter: PlaylistFilter, onFilter: (PlaylistFilter) -> Unit, enabled: Boolean) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        OptionChip(stringResource(R.string.playlist_sort, stringResource(sort.label)),
            PlaylistSort.entries, { it.label }, onSort, enabled)
        OptionChip(stringResource(R.string.playlist_filter, stringResource(filter.label)),
            PlaylistFilter.entries, { it.label }, onFilter, enabled)
    }
}

@Composable
private fun <T> OptionChip(text: String, options: List<T>, label: (T) -> Int, onPick: (T) -> Unit, enabled: Boolean) {
    var open by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    Box {
        AssistChip(
            onClick = { open = true },
            enabled = enabled,
            label = { Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null, Modifier.size(Sizes.iconSmall)) },
            shape = Shapes.pill,
            border = null,
            colors = AssistChipDefaults.assistChipColors(
                containerColor = colors.surfaceContainer,
                labelColor = colors.onSurface,
                trailingIconContentColor = colors.tertiary
            )
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = colors.surfaceContainer) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(stringResource(label(option))) }, onClick = {
                    onPick(option)
                    open = false
                })
            }
        }
    }
}
