package com.example.torrentstreamplayer.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.example.torrentstreamplayer.R
import com.example.torrentstreamplayer.ui.ClipboardLinkPolicy
import kotlinx.coroutines.launch

/**
 * 링크 입력란 옆 [붙여넣기]. 메인 입력 카드와 링크 추가 창이 같이 쓴다.
 *
 * 클립보드는 누를 때만 읽는다(Android 12+는 읽을 때마다 "붙여넣음" 알림을 띄운다). 첫 항목의 글자만 받고
 * (`ClipData.Item.text`: 파일·URI 항목은 열지 않는다), 넣을 글자는 [ClipboardLinkPolicy]가 정한다.
 */
@Composable
fun PasteButton(onPaste: (String) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    SecondaryButton(
        text = stringResource(R.string.action_paste),
        icon = ImageVector.vectorResource(R.drawable.ic_content_paste),
        enabled = enabled,
        onClick = {
            scope.launch {
                val clip = clipboard.getClipEntry()?.clipData
                val text = clip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text
                ClipboardLinkPolicy.pasteText(text)?.let(onPaste)
            }
        },
        modifier = modifier
    )
}
