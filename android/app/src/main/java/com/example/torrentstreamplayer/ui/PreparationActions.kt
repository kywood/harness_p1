package com.example.torrentstreamplayer.ui
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
@Composable
internal fun PreparationActions(onMain: () -> Unit, onPlaylist: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = onMain, modifier = Modifier.weight(1f)) { Text("메인") }
        Button(onClick = onPlaylist, modifier = Modifier.weight(1f)) { Text("재생 목록") }
    }
}
