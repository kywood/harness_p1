package com.example.torrentstreamplayer.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.torrentstreamplayer.R
import com.example.torrentstreamplayer.ui.components.ConfirmDialog

/**
 * 모바일 데이터 재생 확인 창(`design/specs/settings-wifi.md` 5절). Wi-Fi 전용이 켜져 있고 데이터 제한 연결에서 덜 받은
 * 영상을 재생하려 할 때 뜬다. [onPlayOnce]는 이번 재생 한 번만 허용하고 설정은 바꾸지 않는다. [onDismiss]는 "취소"·
 * 바깥 누름·뒤로이며 아무것도 시작하지 않는다.
 */
@Composable
internal fun MobilePlaybackConfirmDialog(onPlayOnce: () -> Unit, onDismiss: () -> Unit) {
    ConfirmDialog(
        title = stringResource(R.string.mobile_play_confirm_title),
        message = stringResource(R.string.mobile_play_confirm_message),
        confirmLabel = stringResource(R.string.action_play_once),
        dismissLabel = stringResource(R.string.action_cancel),
        onConfirm = onPlayOnce,
        onDismiss = onDismiss,
        destructive = false
    )
}
