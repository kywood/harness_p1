package com.example.torrentstreamplayer.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.torrentstreamplayer.R
import com.example.torrentstreamplayer.ui.components.AppBottomSheet
import com.example.torrentstreamplayer.ui.components.RadioListItem

/**
 * "받는 순서" 선택 창(`design/specs/download-order.md` 2절). 설정 화면(두 항목)과 재생 목록 카드(세 항목)가 같이 쓴다.
 * 항목을 누르면 [onSelect]를 부르고 시트를 내린다. 값은 저장에 성공한 뒤 호출하는 쪽에서 바뀐다.
 *
 * @param appDefault 영상별 선택에서 "기본 설정 따름" 옆에 태그로 보일 지금 기본값. 설정 화면은 넘기지 않는다.
 */
@Composable
internal fun DownloadOrderSheet(
    options: List<DownloadOrderOption>,
    selected: DownloadOrderOption,
    onSelect: (DownloadOrderOption) -> Unit,
    onDismiss: () -> Unit,
    appDefault: DownloadOrderOption? = null,
) {
    AppBottomSheet(onDismiss, title = stringResource(R.string.download_order_title), skipPartiallyExpanded = true) { dismiss ->
        Column(Modifier.selectableGroup()) {
            options.forEach { option ->
                RadioListItem(
                    label = stringResource(option.label),
                    selected = option == selected,
                    onClick = {
                        onSelect(option)
                        dismiss()
                    },
                    tag = appDefault?.takeIf { option.preference == null }?.let { stringResource(it.label) },
                    supporting = option.description?.let { stringResource(it) },
                )
            }
        }
    }
}
