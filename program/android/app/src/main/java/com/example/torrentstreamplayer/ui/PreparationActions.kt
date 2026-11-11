package com.example.torrentstreamplayer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.torrentstreamplayer.R
import com.example.torrentstreamplayer.ui.components.PrimaryButton
import com.example.torrentstreamplayer.ui.components.SecondaryButton
import com.example.torrentstreamplayer.ui.theme.Sizes
import com.example.torrentstreamplayer.ui.theme.Spacing

/** 준비 화면 하단 동작: 메인으로 돌아가기, 재생 목록에서 계속 받기. */
@Composable
internal fun PreparationActions(onMain: () -> Unit, onPlaylist: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
        SecondaryButton(stringResource(R.string.action_go_main), onMain,
            Modifier.weight(1f).height(Sizes.primaryButtonHeight))
        PrimaryButton(stringResource(R.string.action_open_playlist), onPlaylist, Modifier.weight(1f))
    }
}
