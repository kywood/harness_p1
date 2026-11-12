package com.example.torrentstreamplayer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.example.torrentstreamplayer.R
import com.example.torrentstreamplayer.ui.UsageGuideSection
import com.example.torrentstreamplayer.ui.components.AppBottomSheet
import com.example.torrentstreamplayer.ui.components.SecondaryButton
import com.example.torrentstreamplayer.ui.components.TextAction
import com.example.torrentstreamplayer.ui.theme.BrandColors
import com.example.torrentstreamplayer.ui.theme.Shapes
import com.example.torrentstreamplayer.ui.theme.Sizes
import com.example.torrentstreamplayer.ui.theme.Spacing

/**
 * 이용 안내 시트(`design/specs/usage-guide.md`, 시안 C). 맨 위 저작권 강조 카드, 앱이 하는 일 절, 개인정보처리방침, 확인.
 *
 * @param openExternal 주소를 외부 브라우저로 연다. 열지 못하면 `false`(시트는 닫지 않고 안에 오류 문구를 보인다).
 */
@Composable
internal fun UsageGuideSheet(onDismiss: () -> Unit, openExternal: (String) -> Boolean) {
    var browserFailed by rememberSaveable { mutableStateOf(false) }
    val privacyUrl = stringResource(R.string.url_privacy_policy)
    AppBottomSheet(onDismiss = onDismiss, title = stringResource(R.string.action_usage_guide),
        skipPartiallyExpanded = true) { dismiss ->
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.screenGutter),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            RightsCard()
            UsageGuideSection.entries.forEach { GuideSection(it) }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                TextAction(
                    text = stringResource(R.string.action_privacy_policy),
                    onClick = { browserFailed = !openExternal(privacyUrl) },
                    trailingIcon = ImageVector.vectorResource(R.drawable.ic_open_in_new),
                    trailingIconDescription = stringResource(R.string.cd_opens_in_browser)
                )
                // 스낵바는 시트 뒤에 가려지므로 시트 안에 문구로 보인다(시안 C 3절).
                if (browserFailed) Text(stringResource(R.string.error_browser_unavailable),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            SecondaryButton(stringResource(R.string.action_confirm), onClick = dismiss, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** 저작권 안내 강조 카드: 시트에서 스크롤 없이 먼저 보인다. */
@Composable
private fun RightsCard() {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = Shapes.medium) {
        Row(Modifier.fillMaxWidth().padding(Spacing.lg), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Icon(Icons.Filled.Info, contentDescription = null, tint = BrandColors.PrimaryLight,
                modifier = Modifier.size(Sizes.icon))
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(stringResource(R.string.main_rights_notice), style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface)
                Text(stringResource(R.string.guide_rights_body), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun GuideSection(section: UsageGuideSection) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(stringResource(section.title), style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.semantics { heading() })
        Text(stringResource(section.body), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
