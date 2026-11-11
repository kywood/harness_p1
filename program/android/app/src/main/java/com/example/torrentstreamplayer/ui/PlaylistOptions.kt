package com.example.torrentstreamplayer.ui
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
@Composable
internal fun PlaylistOptions(sort: PlaylistSort, onSort: (PlaylistSort) -> Unit,
    filter: PlaylistFilter, onFilter: (PlaylistFilter) -> Unit, enabled: Boolean) {
    var sorting by remember { mutableStateOf(false) }
    var filtering by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth()) {
        Box(Modifier.weight(1f)) {
            TextButton(onClick = { sorting = true }, enabled = enabled) { Text("정렬: " + sort.label + " ▾") }
            DropdownMenu(expanded = sorting, onDismissRequest = { sorting = false }) {
                PlaylistSort.entries.forEach { option ->
                    DropdownMenuItem(text = { Text(option.label) }, onClick = { onSort(option); sorting = false })
                }
            }
        }
        Box(Modifier.weight(1f)) {
            TextButton(onClick = { filtering = true }, enabled = enabled) { Text("표시: " + filter.label + " ▾") }
            DropdownMenu(expanded = filtering, onDismissRequest = { filtering = false }) {
                PlaylistFilter.entries.forEach { option ->
                    DropdownMenuItem(text = { Text(option.label) }, onClick = { onFilter(option); filtering = false })
                }
            }
        }
    }
}
