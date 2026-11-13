package com.example.torrentstreamplayer.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.example.torrentstreamplayer.R
import com.example.torrentstreamplayer.meta.PlayerSpec
import com.example.torrentstreamplayer.ui.PlayerNotice
import com.example.torrentstreamplayer.ui.PlayerNoticeQueue
import com.example.torrentstreamplayer.ui.theme.Spacing
import kotlinx.coroutines.delay

/**
 * 재생 화면 알림(시안 3절, PM 결정 5): 자동 자막 선택([subtitles]의 [PlayerSubtitles.autoPicked])과
 * 모바일 데이터 이어 재생([mobilePlayback]이 true가 될 때)을 [PlayerNoticeQueue]로 하나씩 [PlayerSpec.NOTICE_MS] 동안
 * 보인다. 자리는 [PlayerNoticeArea].
 */
@Composable
internal fun PlayerNoticeHost(
    subtitles: PlayerSubtitles?,
    mobilePlayback: Boolean,
    viewportWidthPx: Int,
    panelHeightPx: Int,
    modifier: Modifier,
) {
    var notices by remember { mutableStateOf(PlayerNoticeQueue()) }
    LaunchedEffect(subtitles) {
        subtitles?.autoPicked?.collect { notices = notices.add(PlayerNotice.SubtitleOn(it)) }
    }
    LaunchedEffect(mobilePlayback) { if (mobilePlayback) notices = notices.add(PlayerNotice.MobileData) }
    val current = notices.current
    LaunchedEffect(current) {
        if (current != null) {
            delay(PlayerSpec.NOTICE_MS)
            notices = notices.next()
        }
    }
    PlayerNoticeArea(current, viewportWidthPx, panelHeightPx, modifier)
}

/**
 * 왼쪽 아래 알림 칩 자리(시안 3절): 조작 UI가 보이면 하단 패널([panelHeightPx]) 바로 위, 숨어 있으면 화면 아래에서
 * `Spacing.lg`. 한 번에 하나만 보이고 사라질 때는 마지막 알림을 그대로 흐리게 지운다.
 */
@Composable
private fun PlayerNoticeArea(notice: PlayerNotice?, viewportWidthPx: Int, panelHeightPx: Int, modifier: Modifier) {
    var shown by remember { mutableStateOf<PlayerNotice?>(null) }
    if (notice != null) shown = notice
    val density = LocalDensity.current
    val bottom = if (panelHeightPx > 0) with(density) { panelHeightPx.toDp() } + Spacing.sm else Spacing.lg
    val maxWidth = with(density) { (viewportWidthPx * PlayerSpec.NOTICE_MAX_WIDTH_FRACTION).toDp() }
    AnimatedVisibility(notice != null, modifier.padding(start = Spacing.lg, bottom = bottom),
        enter = fadeIn(), exit = fadeOut()) {
        when (val current = shown) {
            is PlayerNotice.SubtitleOn -> PlayerNoticeChip(
                stringResource(R.string.subtitle_auto_notice, trackNoticeText(current.name)),
                ImageVector.vectorResource(R.drawable.ic_subtitles), maxWidth)
            PlayerNotice.MobileData ->
                PlayerNoticeChip(stringResource(R.string.notice_mobile_playback), null, maxWidth)
            null -> Unit
        }
    }
}
