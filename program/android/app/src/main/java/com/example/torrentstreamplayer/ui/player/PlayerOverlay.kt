package com.example.torrentstreamplayer.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.example.torrentstreamplayer.R
import com.example.torrentstreamplayer.engine.PieceMapSnapshot
import com.example.torrentstreamplayer.ui.PieceMap
import com.example.torrentstreamplayer.ui.VideoOrientationButton
import com.example.torrentstreamplayer.ui.components.AppBottomSheet
import com.example.torrentstreamplayer.ui.components.SecondaryButton
import com.example.torrentstreamplayer.ui.theme.PlaybackColors
import com.example.torrentstreamplayer.ui.theme.Shapes
import com.example.torrentstreamplayer.ui.theme.Sizes
import com.example.torrentstreamplayer.ui.theme.Spacing
import com.example.torrentstreamplayer.ui.theme.TorrentStreamPlayerTheme

/**
 * 재생 화면 위쪽: 상단 가림막 + 뒤로 · 파일 이름 · 회전 · 확대 축소 · 더보기.
 * [stats](속도 · 받은 비율 · 피어)는 가로 화면이면 같은 줄, 세로 화면이면 아래 칩으로 보인다.
 */
@Composable
internal fun PlayerTopBar(
    fileName: String,
    stats: String,
    zoomLabel: String,
    landscape: Boolean,
    onBack: () -> Unit,
    onZoomOut: () -> Unit,
    onZoomReset: () -> Unit,
    onZoomIn: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier.fillMaxWidth().background(PlaybackColors.ScrimTop)
            .padding(horizontal = Spacing.xs, vertical = if (landscape) Spacing.xs else Spacing.sm)
            .padding(bottom = Spacing.xl)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OverlayIconButton(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back), onBack)
            Text(fileName, Modifier.weight(1f).padding(horizontal = Spacing.xs), color = PlaybackColors.OnVideo,
                style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (landscape) StatsChip(stats, Modifier.padding(horizontal = Spacing.xs))
            VideoOrientationButton()
            OverlayIconButton(ImageVector.vectorResource(R.drawable.ic_zoom_out), stringResource(R.string.cd_zoom_out),
                onZoomOut)
            SecondaryLabelButton(zoomLabel, stringResource(R.string.cd_zoom_reset), onZoomReset)
            OverlayIconButton(ImageVector.vectorResource(R.drawable.ic_zoom_in), stringResource(R.string.cd_zoom_in),
                onZoomIn)
            OverlayIconButton(Icons.Filled.MoreVert, stringResource(R.string.cd_more), onMore)
        }
        if (!landscape) StatsChip(stats, Modifier.padding(start = Spacing.md, top = Spacing.xs))
    }
}

/** 가운데: 10초 뒤로 · 재생/일시 정지 · 10초 앞으로. 버퍼링 중이면 위에 안내를 띄운다. */
@Composable
internal fun PlayerCenterControls(
    isPlaying: Boolean,
    waitingMessage: String?,
    onRewind: () -> Unit,
    onTogglePlay: () -> Unit,
    onForward: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
        if (waitingMessage != null) PlayerWaitingPanel(waitingMessage)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xxl), verticalAlignment = Alignment.CenterVertically) {
            RoundControl(ImageVector.vectorResource(R.drawable.ic_replay_10), stringResource(R.string.cd_rewind_10),
                Sizes.playerControl, Sizes.iconMedium, onRewind)
            RoundControl(
                if (isPlaying) ImageVector.vectorResource(R.drawable.ic_pause) else Icons.Filled.PlayArrow,
                stringResource(if (isPlaying) R.string.cd_pause else R.string.cd_play),
                Sizes.playerControlLarge, Sizes.iconLarge, onTogglePlay
            )
            RoundControl(ImageVector.vectorResource(R.drawable.ic_forward_10), stringResource(R.string.cd_forward_10),
                Sizes.playerControl, Sizes.iconMedium, onForward)
        }
    }
}

/** 아래쪽: 하단 가림막 + 시간 · 받은 비율, 재생 막대, 조각 맵. */
@Composable
internal fun PlayerBottomPanel(
    timeLabel: String,
    downloadLabel: String,
    position: Float,
    pieces: PieceMapSnapshot,
    seekEnabled: Boolean,
    landscape: Boolean,
    onSeekChange: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier.fillMaxWidth().background(PlaybackColors.ScrimBottom)
            .padding(horizontal = Spacing.xl).padding(top = Spacing.xl, bottom = if (landscape) Spacing.xs else Spacing.lg)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(timeLabel, Modifier.weight(1f), color = PlaybackColors.OnVideo, style = MaterialTheme.typography.labelMedium)
            Text(downloadLabel, color = PlaybackColors.OnVideoMuted, style = MaterialTheme.typography.labelMedium)
        }
        PlayerSeekBar(position, pieces, seekEnabled, onSeekChange, onSeekFinished)
        PieceMap(pieces, Modifier.fillMaxWidth().padding(bottom = Spacing.xs), PlaybackColors.OnVideoMuted,
            inlineInfo = landscape)
    }
}

/** 조작 UI가 숨었을 때 오른쪽 아래에 작게 남는 시간. */
@Composable
internal fun PlayerTimePill(timeLabel: String, modifier: Modifier = Modifier) {
    Text(timeLabel, color = PlaybackColors.OnVideo, style = MaterialTheme.typography.labelMedium,
        modifier = modifier.padding(Spacing.md).background(PlaybackColors.Panel, Shapes.small)
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs))
}

/** 더보기 하단 시트: 링크 추가, 처음부터 재생. */
@Composable
internal fun PlayerMoreSheet(onDismiss: () -> Unit, onAddLink: (() -> Unit)?, onRestart: () -> Unit) {
    AppBottomSheet(onDismiss = onDismiss) {
        if (onAddLink != null) SheetItem(Icons.Filled.Add, stringResource(R.string.action_add_link), onAddLink)
        SheetItem(Icons.Filled.Refresh, stringResource(R.string.player_restart), onRestart)
    }
}

/** 재생 오류: 경고 아이콘, 오류 문구, 돌아가기. */
@Composable
internal fun PlayerErrorState(message: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.padding(Spacing.xl), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
        Icon(Icons.Filled.Warning, contentDescription = null, tint = PlaybackColors.Error,
            modifier = Modifier.size(Sizes.iconLarge))
        Text(message, color = PlaybackColors.OnVideo, style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center)
        SecondaryButton(stringResource(R.string.action_go_back), onBack)
    }
}

@Composable
private fun PlayerWaitingPanel(message: String) {
    Surface(color = PlaybackColors.Panel, shape = Shapes.medium) {
        Row(Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            CircularProgressIndicator(Modifier.size(Sizes.icon), color = PlaybackColors.Accent,
                trackColor = PlaybackColors.Track)
            Text(message, color = PlaybackColors.OnVideo, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun StatsChip(text: String, modifier: Modifier = Modifier) {
    Text(text, color = PlaybackColors.Accent, style = MaterialTheme.typography.labelMedium, maxLines = 1,
        modifier = modifier.background(PlaybackColors.Chip, Shapes.pill)
            .padding(horizontal = Spacing.md, vertical = Spacing.xs))
}

@Composable
private fun OverlayIconButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(icon, contentDescription = description, tint = PlaybackColors.OnVideo, modifier = Modifier.size(Sizes.icon))
    }
}

@Composable
private fun SecondaryLabelButton(label: String, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.semantics { contentDescription = description }) {
        Text(label, color = PlaybackColors.OnVideo, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

@Composable
private fun RoundControl(
    icon: ImageVector,
    description: String,
    diameter: Dp,
    iconSize: Dp,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick, modifier = Modifier.size(diameter).background(PlaybackColors.ControlButton, CircleShape)) {
        Icon(icon, contentDescription = description, tint = PlaybackColors.OnVideo, modifier = Modifier.size(iconSize))
    }
}

@Composable
private fun SheetItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label, style = MaterialTheme.typography.bodyLarge) },
        leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    )
}

@Preview(widthDp = 411, heightDp = 300)
@Composable
private fun PlayerOverlayPreview() {
    TorrentStreamPlayerTheme {
        Column(Modifier.background(PlaybackColors.Video)) {
            PlayerTopBar("Big.Buck.Bunny.1080p.mkv", "↓ 1.20 MB/s · 35% · 피어 3명", "1.0×", false, {}, {}, {}, {}, {})
            PlayerCenterControls(true, null, {}, {}, {}, Modifier.align(Alignment.CenterHorizontally))
        }
    }
}
