package com.example.torrentstreamplayer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.torrentstreamplayer.ui.theme.Shapes
import com.example.torrentstreamplayer.ui.theme.Spacing
import com.example.torrentstreamplayer.ui.theme.TorrentStreamPlayerTheme

/** 내용 묶음 카드: `surface`, 모서리 `medium`, 안쪽 여백 16dp. [onClick]을 주면 카드 전체가 눌린다. */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val body: @Composable () -> Unit = {
        Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.sm), content = content)
    }
    if (onClick != null) {
        Surface(onClick = onClick, color = MaterialTheme.colorScheme.surface, shape = Shapes.medium,
            modifier = modifier.fillMaxWidth(), content = body)
    } else {
        Surface(color = MaterialTheme.colorScheme.surface, shape = Shapes.medium,
            modifier = modifier.fillMaxWidth(), content = body)
    }
}

@Preview
@Composable
private fun SectionCardPreview() {
    TorrentStreamPlayerTheme {
        SectionCard {
            Text("다운로드 조각", style = MaterialTheme.typography.titleMedium)
            Text("파일 앞 → 뒤", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
