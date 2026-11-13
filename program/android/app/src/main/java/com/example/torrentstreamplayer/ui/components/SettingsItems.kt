package com.example.torrentstreamplayer.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.example.torrentstreamplayer.R
import com.example.torrentstreamplayer.ui.theme.Sizes
import com.example.torrentstreamplayer.ui.theme.Spacing
import com.example.torrentstreamplayer.ui.theme.TorrentStreamPlayerTheme

/** 사용 안 함 상태의 색 투명도(M3 기준 38%, 시안 3절 "사용 안 함: 각 색 38%"). */
private const val DISABLED_ALPHA = 0.38f

/** 버전 값처럼 숫자가 바뀌어도 폭이 흔들리지 않게 하는 고정폭 숫자(OpenType `tnum`). */
private const val TABULAR_NUMBERS = "tnum"

/**
 * 설정 스위치 행(`design/specs/settings-wifi.md` 3절): 제목 + 설명, 오른쪽 스위치. 행 전체가 터치 영역이며 화면 읽기는
 * 행 하나로 합쳐 읽는다. 스위치 자체는 입력을 받지 않는다(두 번 읽힘 방지). 최소 높이 72dp(제목 + 설명 2줄 기준).
 */
@Composable
fun SettingsSwitchItem(
    title: String,
    description: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = Sizes.touchTarget + Spacing.xl)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = Spacing.screenGutter, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            if (description != null) Text(description, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null, enabled = enabled, colors = settingsSwitchColors())
    }
}

/**
 * 스위치 색. 끈 상태 테두리는 `outline`이 아니라 `onSurfaceVariant`다(`outline`은 바탕 대비 약 1.6으로 WCAG 1.4.11 미달).
 */
@Composable
private fun settingsSwitchColors(): SwitchColors {
    val colors = MaterialTheme.colorScheme
    return SwitchDefaults.colors(
        checkedThumbColor = colors.onPrimary,
        checkedTrackColor = colors.primary,
        checkedBorderColor = colors.primary,
        uncheckedThumbColor = colors.onSurfaceVariant,
        uncheckedTrackColor = colors.surfaceContainer,
        uncheckedBorderColor = colors.onSurfaceVariant,
        disabledCheckedThumbColor = colors.onPrimary.copy(alpha = DISABLED_ALPHA),
        disabledCheckedTrackColor = colors.primary.copy(alpha = DISABLED_ALPHA),
        disabledCheckedBorderColor = colors.primary.copy(alpha = DISABLED_ALPHA),
        disabledUncheckedThumbColor = colors.onSurfaceVariant.copy(alpha = DISABLED_ALPHA),
        disabledUncheckedTrackColor = colors.surfaceContainer.copy(alpha = DISABLED_ALPHA),
        disabledUncheckedBorderColor = colors.onSurfaceVariant.copy(alpha = DISABLED_ALPHA),
    )
}

/**
 * 설정 링크 행(`design/specs/settings-wifi.md` 3절). [onClick]이 null이면 누를 수 없는 값 행(버전)이고 끝에 [value]를
 * 보인다. 누를 수 있으면 끝에 화살표, [external]이면 `ic_open_in_new`("브라우저에서 열림")를 둔다. 최소 높이 56dp.
 * 글자가 커져 [value]가 한 줄에 들어가지 않으면 제목 아래 줄로 내려간다.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsLinkItem(
    title: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    external: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val clickModifier = if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier
    FlowRow(
        modifier
            .fillMaxWidth()
            .heightIn(min = Sizes.touchTarget + Spacing.sm)
            .then(clickModifier)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = Spacing.screenGutter, vertical = Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
        verticalArrangement = Arrangement.Center,
        itemVerticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface)
        if (value != null) Text(value,
            style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = TABULAR_NUMBERS),
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (onClick != null) {
            val icon: ImageVector = if (external) ImageVector.vectorResource(R.drawable.ic_open_in_new)
                else Icons.AutoMirrored.Filled.KeyboardArrowRight
            Icon(icon, contentDescription = if (external) stringResource(R.string.cd_opens_in_browser) else null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(Sizes.iconSmall))
        }
    }
}

@Preview
@Composable
private fun SettingsItemsPreview() {
    TorrentStreamPlayerTheme {
        Column {
            SettingsSwitchItem(stringResource(R.string.settings_wifi_only), stringResource(R.string.settings_wifi_only_desc),
                checked = true, onCheckedChange = {})
            SettingsLinkItem(stringResource(R.string.action_privacy_policy), external = true, onClick = {})
            SettingsLinkItem(stringResource(R.string.settings_version), value = "v.2026.11.13.001")
        }
    }
}
