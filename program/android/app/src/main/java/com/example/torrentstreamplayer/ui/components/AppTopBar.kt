package com.example.torrentstreamplayer.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.example.torrentstreamplayer.R
import com.example.torrentstreamplayer.ui.theme.Sizes
import com.example.torrentstreamplayer.ui.theme.Spacing
import com.example.torrentstreamplayer.ui.theme.TorrentStreamPlayerTheme

/**
 * 상단 바: 뒤로 버튼 + 제목 + 오른쪽 동작. `surface` 배경. 상태 표시줄 여백은 바깥 Scaffold가 맡는다.
 * [backLabel]을 주면 뒤로 화살표 대신 그 글자 버튼("닫기")을 둔다.
 */
@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    backLabel: String? = null,
    backEnabled: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxWidth()) {
        Row(
            Modifier.heightIn(min = Sizes.touchTarget + Spacing.lg).padding(horizontal = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null && backLabel != null) {
                TextAction(backLabel, onBack, enabled = backEnabled)
            } else if (onBack != null) {
                IconButton(onClick = onBack, enabled = backEnabled) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                }
            }
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = Spacing.md)
            )
            actions()
        }
    }
}

@Preview
@Composable
private fun AppTopBarPreview() {
    TorrentStreamPlayerTheme { AppTopBar("재생 목록", onBack = {}) }
}
