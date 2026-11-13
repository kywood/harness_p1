package com.example.torrentstreamplayer.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
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
import com.example.torrentstreamplayer.engine.SubtitleFile
import com.example.torrentstreamplayer.meta.PlayerSpec
import com.example.torrentstreamplayer.ui.PlayerNotice
import com.example.torrentstreamplayer.ui.theme.Spacing
import java.io.File

/**
 * 자막 시트에 필요한 값(`design/specs/subtitles.md`). [files]·[pending]은 엔진이 받은(검증된)·받는 중인 자막 파일,
 * [autoSelect]는 설정 "자막 자동 표시", [preferredLanguage]는 기억한 선호 언어. 고른 언어는 [onLanguageChosen]으로 알린다.
 */
internal data class PlayerSubtitleOptions(
    val files: List<SubtitleFile> = emptyList(),
    val pending: List<File> = emptyList(),
    val autoSelect: Boolean = true,
    val preferredLanguage: String? = null,
    val onLanguageChosen: (String) -> Unit = {},
)

/**
 * 왼쪽 아래 알림 칩 자리(시안 3절): 조작 UI가 보이면 하단 패널([panelHeightPx]) 바로 위, 숨어 있으면 화면 아래에서
 * `Spacing.lg`. 한 번에 하나만 보이고 사라질 때는 마지막 알림을 그대로 흐리게 지운다.
 */
@Composable
internal fun PlayerNoticeArea(notice: PlayerNotice?, viewportWidthPx: Int, panelHeightPx: Int, modifier: Modifier) {
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
