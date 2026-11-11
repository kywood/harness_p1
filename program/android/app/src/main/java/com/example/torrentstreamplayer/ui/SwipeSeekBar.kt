package com.example.torrentstreamplayer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.torrentstreamplayer.engine.PieceMapSnapshot
import com.example.torrentstreamplayer.ui.player.PlayerSeekBar
import com.example.torrentstreamplayer.ui.theme.PlaybackColors
import com.example.torrentstreamplayer.ui.theme.Spacing

/** 조작 UI가 숨은 채 가로로 밀어 이동할 때만 잠깐 보이는 재생 막대. 막대 모양은 [PlayerSeekBar]와 같다. */
@Composable
internal fun SwipeSeekBar(position: Float, enabled: Boolean, modifier: Modifier = Modifier,
    onChange: (Float) -> Unit, onFinished: () -> Unit, pieces: PieceMapSnapshot = PieceMapSnapshot.EMPTY) {
    Box(modifier.fillMaxWidth().background(PlaybackColors.ScrimBottom).padding(horizontal = Spacing.xl)) {
        PlayerSeekBar(position, pieces, enabled, onChange, onFinished)
    }
}
