package com.example.torrentstreamplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.example.torrentstreamplayer.ui.theme.BrandColors
import com.example.torrentstreamplayer.ui.theme.Shapes
import com.example.torrentstreamplayer.ui.theme.Sizes
import com.example.torrentstreamplayer.ui.theme.Spacing
import com.example.torrentstreamplayer.ui.theme.TorrentStreamPlayerTheme
import androidx.compose.ui.res.stringResource
import com.example.torrentstreamplayer.R

/**
 * 카드·시트 안 안내 줄(`design/specs/error-recovery.md` 5절): `surfaceContainer` 바탕, `Shapes.small`, 안쪽
 * `Spacing.md`, 앞에 정보 아이콘(`primaryLight`, 장식), 문장 `bodySmall` `onSurface`, 있으면 끝 정렬 글자 버튼.
 * 나타날 때 TalkBack이 읽도록 `liveRegion = Polite`를 건다(준비 화면처럼 시간이 지나 생기는 안내).
 *
 * 쓰는 곳: 준비 화면 피어 0명 안내, 링크 확인 시트, 설정 화면 Wi-Fi 안내.
 *
 * @param actionLabel 글자 버튼 문구. [onAction]과 함께 넘겨야 버튼이 보인다.
 */
@Composable
fun InlineNotice(
    text: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, Shapes.small)
            .padding(Spacing.md)
    ) {
        Row(
            Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Icon(Icons.Filled.Info, contentDescription = null, tint = BrandColors.PrimaryLight,
                modifier = Modifier.size(Sizes.iconSmall))
            Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f))
        }
        if (actionLabel != null && onAction != null) {
            TextAction(actionLabel, onAction, Modifier.align(Alignment.End))
        }
    }
}

@Preview
@Composable
private fun InlineNoticePreview() {
    TorrentStreamPlayerTheme {
        Column(
            Modifier.background(MaterialTheme.colorScheme.surface).padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            InlineNotice(stringResource(R.string.open_link_in_playlist))
            InlineNotice(
                stringResource(R.string.settings_wifi_paused_notice),
                actionLabel = stringResource(R.string.action_open_playlist),
                onAction = {}
            )
        }
    }
}
