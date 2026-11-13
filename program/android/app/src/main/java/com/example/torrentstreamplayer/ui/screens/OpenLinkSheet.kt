package com.example.torrentstreamplayer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.example.torrentstreamplayer.R
import com.example.torrentstreamplayer.engine.MagnetIdentity
import com.example.torrentstreamplayer.ui.DownloadEntry
import com.example.torrentstreamplayer.ui.IncomingLinkAction
import com.example.torrentstreamplayer.ui.IncomingLinkPolicy
import com.example.torrentstreamplayer.ui.IncomingLinkState
import com.example.torrentstreamplayer.ui.IncomingLinkSummary
import com.example.torrentstreamplayer.ui.LinkSheetButton
import com.example.torrentstreamplayer.ui.MainViewModel
import com.example.torrentstreamplayer.ui.PlaylistItemStatus
import com.example.torrentstreamplayer.ui.components.AppBottomSheet
import com.example.torrentstreamplayer.ui.components.InlineNotice
import com.example.torrentstreamplayer.ui.components.PrimaryButton
import com.example.torrentstreamplayer.ui.components.SecondaryButton
import com.example.torrentstreamplayer.ui.components.StatusChip
import com.example.torrentstreamplayer.ui.components.TextAction
import com.example.torrentstreamplayer.ui.components.TextActionTone
import com.example.torrentstreamplayer.ui.formatTime
import com.example.torrentstreamplayer.ui.theme.BrandColors
import com.example.torrentstreamplayer.ui.theme.Shapes
import com.example.torrentstreamplayer.ui.theme.Sizes
import com.example.torrentstreamplayer.ui.theme.Spacing
import kotlinx.coroutines.flow.filterNotNull

/**
 * 링크 확인 시트와 "받아 두기" 뒤 스낵바를 띄운다. 메인·준비·재생 어느 화면 위에서든 같은 시트다
 * (`design/specs/open-link-sheet.md`). [AppRoot]의 바깥 상자 안에 둔다.
 */
@Composable
internal fun OpenLinkHost(viewModel: MainViewModel, downloadJobs: Map<String, DownloadEntry>, modifier: Modifier) {
    val state by viewModel.incomingLink.collectAsState()
    val activeKey by viewModel.activeKey.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    // 키가 바뀌지 않는 효과 하나가 알림을 차례로 받는다. 스낵바가 끝난 뒤에야 "보였다"고 알려, 알림 값이 바뀌어도
    // 떠 있는 스낵바가 취소되지 않는다.
    LaunchedEffect(viewModel) {
        viewModel.incomingLinkNotice.filterNotNull().collect { event ->
            val result = snackbar.showSnackbar(context.getString(event.notice.message),
                actionLabel = event.action?.let(context::getString))
            viewModel.incomingLinkNoticeShown(event)
            if (result == SnackbarResult.ActionPerformed) viewModel.openPlaylist()
        }
    }
    when (val current = state) {
        null -> Unit
        // 링크마다 새 시트: 앞 시트가 닫히는 동안 들어온 링크도 펼친 시트로 보인다.
        is IncomingLinkState.Rejected -> key(current) {
            RejectedLinkSheet(current, onDismiss = { viewModel.dismissIncomingLink(current) })
        }
        is IncomingLinkState.Confirm -> key(current.link) {
            val key = current.match?.let { runCatching { MagnetIdentity.key(it.magnet) }.getOrNull() }
            val task = key?.let(downloadJobs::get)
            val status = current.match?.let { IncomingLinkPolicy.status(it, task, playing = key == activeKey) }
            OpenLinkSheet(current, task, status,
                onPlay = { viewModel.playIncomingLink() },
                onSave = viewModel::saveIncomingLink,
                onDismiss = { viewModel.dismissIncomingLink(current) })
        }
    }
    SnackbarHost(snackbar, modifier)
}

/**
 * 확인 시트(1절, 2-1, 2-3). 버튼은 [IncomingLinkPolicy.buttons]가 정하고, 시트가 바쁜 동안(재생 목록 조회·받아 두기)
 * "취소" 말고는 누를 수 없다.
 *
 * @param task 재생 목록 항목의 다운로드 작업. 받는 중이면 "받아 두기"를 숨긴다.
 * @param status 재생 목록 항목의 상태 칩. 재생 목록에 없으면 null.
 */
@Composable
internal fun OpenLinkSheet(
    state: IncomingLinkState.Confirm,
    task: DownloadEntry?,
    status: PlaylistItemStatus?,
    onPlay: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    HideKeyboard()
    val buttons = IncomingLinkPolicy.buttons(state, task?.phase)
    AppBottomSheet(onDismiss = onDismiss, title = stringResource(R.string.open_link_title),
        skipPartiallyExpanded = true) { dismiss ->
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = Spacing.screenGutter),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            LinkSummaryCard(state, status)
            if (state.match != null) InlineNotice(stringResource(R.string.open_link_in_playlist))
            if (IncomingLinkAction.PLAY_NOW !in state.allowed) {
                Text(stringResource(R.string.open_link_keep_playing), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                buttons.primary?.let { button ->
                    PrimaryButton(stringResource(button.label), onClick = button.action(onPlay, onSave),
                        modifier = Modifier.fillMaxWidth().saveDescription(button), enabled = buttons.enabled,
                        icon = button.icon())
                }
                buttons.secondary?.let { button ->
                    SecondaryButton(stringResource(button.label), onClick = button.action(onPlay, onSave),
                        modifier = Modifier.fillMaxWidth().saveDescription(button), enabled = buttons.enabled,
                        icon = button.icon())
                }
                TextAction(stringResource(R.string.action_cancel), onClick = dismiss, tone = TextActionTone.MUTED,
                    modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        }
    }
}

/** 오류 시트(2-2): 이유 문장과 "확인" 하나. 메인 입력란은 채우지 않는다. */
@Composable
internal fun RejectedLinkSheet(state: IncomingLinkState.Rejected, onDismiss: () -> Unit) {
    HideKeyboard()
    AppBottomSheet(onDismiss = onDismiss, title = stringResource(R.string.open_link_error_title),
        skipPartiallyExpanded = true) { dismiss ->
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = Spacing.screenGutter),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(Sizes.iconSmall))
                Text(stringResource(state.reason.message), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            }
            SecondaryButton(stringResource(R.string.action_confirm), onClick = dismiss, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** 요약 카드: 이름(또는 줄인 해시)과 종류, 재생 목록에 있으면 상태 칩과 이어보기 위치. 한 덩어리로 읽힌다. */
@Composable
private fun LinkSummaryCard(state: IncomingLinkState.Confirm, status: PlaylistItemStatus?) {
    val summary = IncomingLinkSummary.of(state.link, state.match?.title)
    val name = summary.name ?: stringResource(R.string.open_link_magnet_short, summary.hashPrefix.orEmpty())
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = Shapes.medium,
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {
        Row(Modifier.padding(Spacing.lg), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Icon(ImageVector.vectorResource(R.drawable.ic_movie), contentDescription = null,
                tint = BrandColors.PrimaryLight, modifier = Modifier.size(Sizes.icon))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (summary.showKind) Text(stringResource(summary.kind.label),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (status != null) PlaylistLine(status, state.match?.positionMs ?: 0)
            }
        }
    }
}

@Composable
private fun PlaylistLine(status: PlaylistItemStatus, positionMs: Long) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        StatusChip(stringResource(status.label), status.tone)
        if (positionMs > 0) Text(stringResource(R.string.playlist_resume, formatTime(positionMs)),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** 시트가 뜨면 메인 입력란 키보드를 내린다(2-4 "키보드"). */
@Composable
private fun HideKeyboard() {
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { keyboard?.hide() }
}

private fun LinkSheetButton.action(onPlay: () -> Unit, onSave: () -> Unit): () -> Unit = when (this) {
    LinkSheetButton.PLAY_NOW, LinkSheetButton.RESUME -> onPlay
    LinkSheetButton.SAVE -> onSave
}

@Composable
private fun LinkSheetButton.icon(): ImageVector = when (this) {
    LinkSheetButton.PLAY_NOW, LinkSheetButton.RESUME -> Icons.Filled.PlayArrow
    LinkSheetButton.SAVE -> ImageVector.vectorResource(R.drawable.ic_download)
}

/** "받아 두기"는 어디에 쌓이는지까지 읽는다("받아 두기, 재생 목록에 추가하고 다운로드", 5절). */
@Composable
private fun Modifier.saveDescription(button: LinkSheetButton): Modifier {
    if (button != LinkSheetButton.SAVE) return this
    val description = stringResource(R.string.cd_save_for_later)
    return semantics { contentDescription = description }
}
