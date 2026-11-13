package com.example.torrentstreamplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.example.torrentstreamplayer.ui.theme.Shapes
import com.example.torrentstreamplayer.ui.theme.Spacing
import com.example.torrentstreamplayer.ui.theme.TorrentStreamPlayerTheme

/**
 * 고르기 목록의 행 하나(`design/specs/subtitles.md` 5절). 행 전체가 터치 영역(`selectable`, 라디오 버튼 역할)이고
 * 최소 높이는 `ListItem` 한 줄 높이다. 묶음은 호출하는 쪽이 `Modifier.selectableGroup()`으로 감싼다.
 *
 * @param tag 글자 오른쪽 태그("파일", "받는 중"). 글자가 길어도 줄어들지 않는다.
 * @param description 있으면 행을 이 이름으로만 읽는다(보이는 글자와 태그 대신). 보이는 글자를 앞에 둔다(WCAG 2.5.3).
 */
@Composable
fun RadioListItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tag: String? = null,
    description: String? = null,
) {
    val colors = MaterialTheme.colorScheme
    val named = if (description != null) Modifier.semantics { contentDescription = description } else Modifier
    ListItem(
        headlineContent = {
            Text(label, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = if (description != null) Modifier.clearAndSetSemantics {} else Modifier)
        },
        leadingContent = {
            RadioButton(
                selected = selected,
                onClick = null,
                enabled = enabled,
                colors = RadioButtonDefaults.colors(
                    selectedColor = colors.tertiary,
                    unselectedColor = colors.onSurfaceVariant,
                    disabledSelectedColor = colors.onSurfaceVariant,
                    disabledUnselectedColor = colors.onSurfaceVariant,
                ),
            )
        },
        trailingContent = tag?.let { text ->
            {
                Text(text, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant, maxLines = 1,
                    modifier = (if (description != null) Modifier.clearAndSetSemantics {} else Modifier)
                        .background(colors.surfaceContainer, Shapes.small)
                        .padding(horizontal = Spacing.sm))
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = colors.surface,
            // 고를 수 없는 행도 38% 흐림 대신 onSurfaceVariant로 AA 대비를 지킨다(시안 6절).
            headlineColor = if (enabled) colors.onSurface else colors.onSurfaceVariant,
        ),
        modifier = modifier
            .fillMaxWidth()
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .then(named),
    )
}

@Preview
@Composable
private fun RadioListItemPreview() {
    TorrentStreamPlayerTheme {
        Column(Modifier.selectableGroup()) {
            RadioListItem("끄기", selected = false, onClick = {})
            RadioListItem("한국어", selected = true, onClick = {})
            RadioListItem("Movie.Name.2024.ko.srt", selected = false, onClick = {}, tag = "파일")
            RadioListItem("Movie.Name.2024.en.smi", selected = false, onClick = {}, enabled = false, tag = "받는 중")
        }
    }
}
