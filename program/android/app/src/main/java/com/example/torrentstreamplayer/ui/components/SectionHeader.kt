package com.example.torrentstreamplayer.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.example.torrentstreamplayer.ui.theme.Spacing
import com.example.torrentstreamplayer.ui.theme.TorrentStreamPlayerTheme
import androidx.compose.ui.res.stringResource
import com.example.torrentstreamplayer.R

/**
 * 목록 안 묶음 제목("다운로드", "재생", "정보"). `titleSmall` `onSurfaceVariant`, 화면 읽기는 제목으로 읽는다.
 * 위 간격은 [top](첫 묶음은 `Spacing.lg`, 나머지 `Spacing.xl`), 아래는 `Spacing.xs`(`design/specs/settings-wifi.md` 2절).
 */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, top: Dp = Spacing.xl) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = Spacing.screenGutter, end = Spacing.screenGutter, top = top, bottom = Spacing.xs)
            .semantics { heading() }
    )
}

@Preview
@Composable
private fun SectionHeaderPreview() {
    TorrentStreamPlayerTheme { SectionHeader(stringResource(R.string.settings_section_download), top = Spacing.lg) }
}
