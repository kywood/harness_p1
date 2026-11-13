package com.example.torrentstreamplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.example.torrentstreamplayer.R
import com.example.torrentstreamplayer.engine.MetadataDetails
import com.example.torrentstreamplayer.engine.PieceMapSnapshot
import com.example.torrentstreamplayer.ui.MetadataDetailsPanel
import com.example.torrentstreamplayer.ui.PieceMap
import com.example.torrentstreamplayer.ui.PreparationActions
import com.example.torrentstreamplayer.ui.StreamUiState
import com.example.torrentstreamplayer.ui.components.ProgressBar
import com.example.torrentstreamplayer.ui.components.SectionCard
import com.example.torrentstreamplayer.ui.components.StatusChip
import com.example.torrentstreamplayer.ui.components.StatusTone
import com.example.torrentstreamplayer.ui.navigation.AppScreen
import com.example.torrentstreamplayer.ui.theme.BrandColors
import com.example.torrentstreamplayer.ui.theme.Shapes
import com.example.torrentstreamplayer.ui.theme.Sizes
import com.example.torrentstreamplayer.ui.theme.Spacing
import java.util.Locale

/** [StreamUiState.Buffering.progress]는 백분율(0..100)이다. */
private const val PERCENT = 100f
private const val BYTES_PER_KB = 1024L

/** 파일 이름 최대 줄 수. */
private const val FILE_NAME_MAX_LINES = 2

/** 준비 화면: 파일 이름, 단계(정보 → 연결 → 버퍼링), 진행 막대와 칩, 조각 맵, 접는 세부 정보, 하단 동작. */
@Composable
internal fun PreparingScreen(
    screen: AppScreen.Preparing,
    metadata: MetadataDetails,
    pieces: PieceMapSnapshot,
    onCancel: () -> Unit,
    onOpenPlaylist: () -> Unit
) {
    val buffering = (screen as? AppScreen.Preparing.Buffering)?.stream
    val fileName = buffering?.fileName ?: metadata.name ?: stringResource(R.string.preparing_name_pending)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screenGutter, vertical = Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            Modifier.widthIn(max = Sizes.contentMaxWidth).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            Text(fileName, style = MaterialTheme.typography.titleMedium, maxLines = FILE_NAME_MAX_LINES,
                overflow = TextOverflow.Ellipsis)
            StepIndicator(PreparingStep.of(screen, metadata.hasMetadata))
            ProgressCard(buffering, metadata)
            SectionCard { PieceMap(pieces, Modifier.fillMaxWidth()) }
            DetailsCard(metadata)
            PreparationActions(onCancel, onOpenPlaylist)
        }
    }
}

/** 단계 하나의 표시 상태. 점과 글자 색을 함께 등록한다. */
private enum class StepState(val dot: Color, val label: Color, val weight: FontWeight) {
    DONE(BrandColors.Accent, BrandColors.OnSurfaceVariant, FontWeight.Normal),
    ACTIVE(BrandColors.PrimaryLight, BrandColors.OnSurface, FontWeight.SemiBold),
    PENDING(BrandColors.Outline, BrandColors.OnSurfaceVariant, FontWeight.Normal);

    companion object {
        fun of(step: PreparingStep, current: PreparingStep): StepState = when {
            step.ordinal < current.ordinal -> DONE
            step == current -> ACTIVE
            else -> PENDING
        }
    }
}

@Composable
private fun StepIndicator(current: PreparingStep) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        PreparingStep.entries.forEachIndexed { index, step ->
            val state = StepState.of(step, current)
            if (index > 0) {
                Box(
                    Modifier.weight(1f).padding(horizontal = Spacing.sm).height(Spacing.xs / 2)
                        .background(if (state == StepState.PENDING) BrandColors.Outline else BrandColors.Accent)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Box(Modifier.size(Spacing.sm + Spacing.xs / 2).background(state.dot, CircleShape))
                Text(stringResource(step.label), style = MaterialTheme.typography.labelLarge, color = state.label,
                    fontWeight = state.weight, maxLines = 1)
            }
        }
    }
}

@Composable
private fun ProgressCard(buffering: StreamUiState.Buffering?, metadata: MetadataDetails) {
    SectionCard {
        if (buffering != null) {
            val percent = buffering.progress.toInt()
            Text(
                stringResource(if (buffering.autoPlay) R.string.label_buffering else R.string.label_downloading) +
                    ": $percent%",
                style = MaterialTheme.typography.bodyMedium
            )
            ProgressBar(buffering.progress / PERCENT)
        } else {
            Text(
                stringResource(if (metadata.hasMetadata) R.string.title_metadata_received
                    else R.string.title_metadata_fetching),
                style = MaterialTheme.typography.bodyMedium
            )
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(Sizes.progressBarHeight).clip(Shapes.pill),
                color = MaterialTheme.colorScheme.tertiary,
                trackColor = MaterialTheme.colorScheme.outline,
                gapSize = Spacing.xs / 2
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            if (buffering != null) StatusChip(speedText(buffering.downloadSpeedBytes), StatusTone.DOWNLOADING)
            StatusChip(pluralStringResource(R.plurals.label_peers, metadata.peers, metadata.peers),
                if (metadata.peers > 0) StatusTone.COMPLETED else StatusTone.NEUTRAL)
        }
        if (buffering == null) {
            Text(stringResource(R.string.subtitle_metadata_fetching), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** 기존 준비 화면과 같은 속도 표기(1MB/s 이상은 소수 둘째 자리). 단위 기호라 번역하지 않는다. */
private fun speedText(bytesPerSecond: Long): String {
    val kb = bytesPerSecond / BYTES_PER_KB
    val mb = kb / BYTES_PER_KB.toDouble()
    return if (mb >= 1.0) String.format(Locale.US, "%.2f MB/s", mb) else "$kb KB/s"
}

@Composable
private fun DetailsCard(metadata: MetadataDetails) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    SectionCard {
        Row(
            Modifier.fillMaxWidth().heightIn(min = Sizes.touchTarget)
                .clickable(role = Role.Button, onClickLabel = stringResource(
                    if (expanded) R.string.cd_collapse else R.string.cd_expand)) { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.preparing_details), style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f))
            Icon(
                if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = stringResource(if (expanded) R.string.cd_collapse else R.string.cd_expand),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (expanded) MetadataDetailsPanel(metadata)
    }
}
