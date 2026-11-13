package com.example.torrentstreamplayer.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.torrentstreamplayer.R
import com.example.torrentstreamplayer.ui.OptionTag
import com.example.torrentstreamplayer.ui.PlayerTracks
import com.example.torrentstreamplayer.ui.SubtitleSheetState
import com.example.torrentstreamplayer.ui.TrackLanguage
import com.example.torrentstreamplayer.ui.TrackName
import com.example.torrentstreamplayer.ui.TrackOption
import com.example.torrentstreamplayer.ui.components.AppBottomSheet
import com.example.torrentstreamplayer.ui.components.RadioListItem
import com.example.torrentstreamplayer.ui.components.SectionHeader
import com.example.torrentstreamplayer.ui.theme.Sizes
import com.example.torrentstreamplayer.ui.theme.Spacing
import com.example.torrentstreamplayer.ui.theme.TorrentStreamPlayerTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * 자막·음성 시트(`design/specs/subtitles.md` 2절). 반 펼침으로 열어 위쪽 영상이 보이게 하고, 행을 누르면 바로 바꾸되
 * 시트는 닫지 않는다. [selectFailed]가 오면 시트 안 스낵바로 "다시 골라 주세요"를 알린다.
 */
@Composable
internal fun SubtitleSheet(
    state: SubtitleSheetState,
    onDismiss: () -> Unit,
    onSelectSubtitle: (Int) -> Unit,
    onSelectAudio: (Int) -> Unit,
    selectFailed: Flow<Unit> = emptyFlow(),
) {
    val snackbar = remember { SnackbarHostState() }
    val resources = LocalResources.current
    LaunchedEffect(selectFailed) {
        selectFailed.collect { snackbar.showSnackbar(resources.getString(R.string.error_track_select)) }
    }
    AppBottomSheet(onDismiss = onDismiss, skipPartiallyExpanded = false) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            SectionHeader(stringResource(R.string.label_subtitles), top = Spacing.sm)
            when {
                state.loading -> TracksLoading()
                state.subtitles.isEmpty() -> Text(stringResource(R.string.subtitle_none),
                    style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = Spacing.screenGutter, vertical = Spacing.lg))
                else -> Column(Modifier.selectableGroup()) {
                    RadioListItem(stringResource(R.string.subtitle_off),
                        selected = state.selectedSubtitle == PlayerTracks.NO_TRACK,
                        onClick = { onSelectSubtitle(PlayerTracks.NO_TRACK) })
                    state.subtitles.forEach { option ->
                        TrackRow(option, option.enabled && option.id == state.selectedSubtitle, onSelectSubtitle)
                    }
                }
            }
            if (state.audio.isNotEmpty()) {
                HorizontalDivider(Modifier.padding(vertical = Spacing.sm), color = MaterialTheme.colorScheme.outline)
                SectionHeader(stringResource(R.string.label_audio), top = Spacing.sm)
                Column(Modifier.selectableGroup()) {
                    state.audio.forEach { option -> TrackRow(option, option.id == state.selectedAudio, onSelectAudio) }
                }
            }
        }
        SnackbarHost(snackbar)
    }
}

/** 트랙 이름을 화면 문구로. 외부 자막은 파일 이름 그대로다. */
@Composable
internal fun trackNameText(name: TrackName): String = when (name) {
    is TrackName.Language -> stringResource(name.language.label)
    is TrackName.LanguageDetail ->
        stringResource(R.string.track_with_detail, stringResource(name.language.label), name.detail)
    is TrackName.LanguageNumber ->
        stringResource(R.string.track_duplicate, stringResource(name.language.label), name.number)
    is TrackName.Text -> name.text
    is TrackName.Untitled -> stringResource(R.string.track_untitled, name.number)
    is TrackName.File -> name.fileName
}

/** 알림 칩·상단 버튼 읽기 문구용 이름. 외부 자막은 "한국어(파일)", 언어를 모르면 "파일 이름(파일)"(시안 3절). */
@Composable
internal fun trackNoticeText(name: TrackName): String = when (name) {
    is TrackName.File -> stringResource(R.string.subtitle_file_suffix,
        name.language?.let { stringResource(it.label) } ?: name.fileName)
    else -> trackNameText(name)
}

@Composable
private fun TrackRow(option: TrackOption, selected: Boolean, onSelect: (Int) -> Unit) {
    val label = trackNameText(option.name)
    val readsAsFile = option.tag == OptionTag.FILE && option.enabled
    RadioListItem(
        label = label,
        selected = selected,
        onClick = { onSelect(option.id) },
        enabled = option.enabled,
        tag = option.tag?.let { stringResource(it.label) },
        description = if (readsAsFile) stringResource(R.string.cd_subtitle_file, label) else null,
    )
}

@Composable
private fun TracksLoading() {
    Row(Modifier.fillMaxWidth().padding(horizontal = Spacing.screenGutter, vertical = Spacing.lg),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
        CircularProgressIndicator(Modifier.size(Sizes.iconSmall), color = MaterialTheme.colorScheme.tertiary)
        Text(stringResource(R.string.tracks_loading), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Preview
@Composable
private fun SubtitleSheetContentPreview() {
    TorrentStreamPlayerTheme {
        Column(Modifier.selectableGroup()) {
            TrackRow(TrackOption(1, TrackName.Language(TrackLanguage.KOREAN)), selected = true, onSelect = {})
            TrackRow(TrackOption(2, TrackName.File("Movie.Name.2024.ko.srt", TrackLanguage.KOREAN), OptionTag.FILE),
                selected = false, onSelect = {})
            TrackRow(TrackOption(-1, TrackName.File("Movie.Name.2024.en.smi", null), OptionTag.DOWNLOADING, false),
                selected = false, onSelect = {})
        }
    }
}
