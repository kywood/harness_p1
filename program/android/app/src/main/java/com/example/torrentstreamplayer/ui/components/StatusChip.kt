package com.example.torrentstreamplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import com.example.torrentstreamplayer.ui.theme.BrandColors
import com.example.torrentstreamplayer.ui.theme.Shapes
import com.example.torrentstreamplayer.ui.theme.Sizes
import com.example.torrentstreamplayer.ui.theme.Spacing
import com.example.torrentstreamplayer.ui.theme.TorrentStreamPlayerTheme

/**
 * 상태 칩의 종류. 종류마다 점(또는 아이콘) 색을 등록한다. 글자는 색만으로 상태를 전하지 않도록 호출하는 쪽이 넘긴다.
 */
enum class StatusTone(val dot: Color) {
    NEUTRAL(BrandColors.OnSurfaceVariant),
    DOWNLOADING(BrandColors.PrimaryLight),
    QUEUED(BrandColors.OnSurfaceVariant),
    COMPLETED(BrandColors.Accent),
    FAILED(BrandColors.Error),

    /** "Wi-Fi 대기": 대기와 같은 회색 계열, `ic_wifi` 아이콘과 글자로 구분한다(`design/specs/settings-wifi.md` 4절). */
    WAITING_WIFI(BrandColors.OnSurfaceVariant),
}

/**
 * 다운로드 중·대기·완료·실패 또는 속도·피어 같은 짧은 상태 표시.
 * [icon]이 있으면 점 대신 그 아이콘(`Sizes.iconSmall`, [tone] 색)을 둔다. 아이콘은 장식이라 읽지 않는다.
 */
@Composable
fun StatusChip(label: String, tone: StatusTone, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Row(
        modifier
            .background(MaterialTheme.colorScheme.surfaceContainer, Shapes.pill)
            .padding(horizontal = Spacing.md, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = tone.dot, modifier = Modifier.size(Sizes.iconSmall))
        else Box(Modifier.size(Spacing.sm).background(tone.dot, CircleShape))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1)
    }
}

@Preview
@Composable
private fun StatusChipPreview() {
    TorrentStreamPlayerTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            StatusChip("다운로드 중", StatusTone.DOWNLOADING)
            StatusChip("완료", StatusTone.COMPLETED)
            StatusChip("실패", StatusTone.FAILED)
        }
    }
}
