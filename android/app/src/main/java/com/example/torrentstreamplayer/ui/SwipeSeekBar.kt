package com.example.torrentstreamplayer.ui
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.torrentstreamplayer.ui.theme.PlaybackColors
@Composable
internal fun SwipeSeekBar(position: Float, enabled: Boolean, modifier: Modifier = Modifier,
    onChange: (Float) -> Unit, onFinished: () -> Unit) {
    Box(modifier.fillMaxWidth().background(PlaybackColors.Panel).padding(horizontal = 24.dp)) {
        Slider(value = position.coerceIn(0f, 1f), onValueChange = onChange,
            onValueChangeFinished = onFinished, enabled = enabled,
            colors = SliderDefaults.colors(thumbColor = PlaybackColors.Accent,
                activeTrackColor = PlaybackColors.Accent, inactiveTrackColor = PlaybackColors.Track),
            modifier = Modifier.fillMaxWidth())
    }
}
