package com.example.torrentstreamplayer.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.example.torrentstreamplayer.ui.theme.Sizes
import com.example.torrentstreamplayer.ui.theme.Spacing
import kotlinx.coroutines.launch

/**
 * 공용 하단 시트(`design/specs/usage-guide.md` 4절). 바탕 `surface`, 최대 폭 [Sizes.contentMaxWidth],
 * 아래 시스템 내비게이션 여백 + `Spacing.lg`. 가로 여백은 내용이 정한다(목록 행은 자체 여백이 있다).
 *
 * @param title 있으면 맨 위에 `titleLarge` 제목(TalkBack 제목)을 둔다.
 * @param skipPartiallyExpanded `true`면 반만 펼친 단계 없이 바로 전체로 펼친다.
 * @param content 받는 함수를 부르면 시트를 내리는 애니메이션 뒤 [onDismiss]가 불린다("확인" 버튼 등).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBottomSheet(
    onDismiss: () -> Unit,
    title: String? = null,
    skipPartiallyExpanded: Boolean = false,
    content: @Composable ColumnScope.(dismiss: () -> Unit) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = skipPartiallyExpanded)
    val scope = rememberCoroutineScope()
    val dismiss: () -> Unit = {
        scope.launch { sheetState.hide() }.invokeOnCompletion { if (!sheetState.isVisible) onDismiss() }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        sheetMaxWidth = Sizes.contentMaxWidth,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = Spacing.lg)) {
            if (title != null) Text(title, style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = Spacing.screenGutter).padding(bottom = Spacing.lg)
                    .semantics { heading() })
            content(dismiss)
        }
    }
}
