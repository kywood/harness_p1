package com.example.torrentstreamplayer.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import com.example.torrentstreamplayer.R
import com.example.torrentstreamplayer.ui.LegalFooterPolicy
import com.example.torrentstreamplayer.ui.LegalLink
import com.example.torrentstreamplayer.ui.MainInputFill
import com.example.torrentstreamplayer.ui.components.LinkTextField
import com.example.torrentstreamplayer.ui.components.PrimaryButton
import com.example.torrentstreamplayer.ui.components.SecondaryButton
import com.example.torrentstreamplayer.ui.components.SectionCard
import com.example.torrentstreamplayer.ui.components.TextAction
import com.example.torrentstreamplayer.ui.components.TextActionTone
import com.example.torrentstreamplayer.ui.components.rememberExternalLinkOpener
import com.example.torrentstreamplayer.ui.theme.BrandColors
import com.example.torrentstreamplayer.ui.theme.Shapes
import com.example.torrentstreamplayer.ui.theme.Sizes
import com.example.torrentstreamplayer.ui.theme.Spacing
import kotlinx.coroutines.launch

/**
 * 메인: 링크를 넣고 바로 재생하거나 재생 목록·저장 파일 관리로 간다.
 *
 * @param inputFill 다른 앱에서 받은 링크로 입력란을 채우거나(받아 두기 뒤에는) 비운다. 한 번 반영하고
 * [onInputFilled]로 알려, 사용자가 지운 뒤 다른 화면을 다녀와도 다시 채우지 않는다.
 */
@Composable
internal fun MagnetLinkInputScreen(
    onStartStream: (String) -> Unit,
    errorMessage: String?,
    onOpenFiles: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
    onOpenLicenses: () -> Unit = {},
    onPrivacyOptions: (() -> Unit)? = null,
    inputFill: MainInputFill? = null,
    onInputFilled: (MainInputFill) -> Unit = {}
) {
    var magnetLink by remember { mutableStateOf("") }
    LaunchedEffect(inputFill) {
        inputFill?.let {
            magnetLink = it.text
            onInputFilled(it)
        }
    }
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val versionName = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull()
    }
    val snackbar = remember { SnackbarHostState() }
    val openExternal = rememberExternalLinkOpener()
    val browserUnavailable = stringResource(R.string.error_browser_unavailable)
    var showUsageGuide by rememberSaveable { mutableStateOf(false) }
    val footerActions = FooterActions(
        openUsageGuide = { showUsageGuide = true },
        openExternal = { url -> if (!openExternal(url)) scope.launch { snackbar.showSnackbar(browserUnavailable) } },
        openLicenses = onOpenLicenses,
        privacyOptions = onPrivacyOptions
    )
    if (showUsageGuide) UsageGuideSheet(onDismiss = { showUsageGuide = false }, openExternal = openExternal)

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenGutter, vertical = Spacing.xl),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                Modifier.widthIn(max = Sizes.contentMaxWidth).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BrandHeader()
                SectionCard {
                    LinkTextField(magnetLink, { magnetLink = it }, errorMessage)
                    SecondaryButton(
                        text = stringResource(R.string.action_paste),
                        icon = ImageVector.vectorResource(R.drawable.ic_content_paste),
                        onClick = {
                            scope.launch {
                                val clip = clipboard.getClipEntry()?.clipData
                                val text = clip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)
                                if (!text.isNullOrBlank()) magnetLink = text.toString().trim()
                            }
                        },
                        modifier = Modifier.align(Alignment.End)
                    )
                }
                PrimaryButton(
                    text = stringResource(R.string.action_stream_play),
                    onClick = { onStartStream(magnetLink) },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    ShortcutCard(Icons.AutoMirrored.Filled.List, stringResource(R.string.action_open_playlist),
                        stringResource(R.string.main_playlist_hint), onOpenHistory, Modifier.weight(1f))
                    ShortcutCard(ImageVector.vectorResource(R.drawable.ic_folder),
                        stringResource(R.string.action_open_stored_files), stringResource(R.string.main_files_hint),
                        onOpenFiles, Modifier.weight(1f))
                }
                Footer(versionName, footerActions)
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun BrandHeader() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Image(
            painter = painterResource(R.drawable.pieceplay_icon_foreground),
            contentDescription = stringResource(R.string.cd_app_logo),
            modifier = Modifier.size(Sizes.logo).clip(Shapes.large)
        )
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.main_tagline),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/** 메인 아래 바로가기 카드(재생 목록·저장 파일 관리). */
@Composable
private fun ShortcutCard(icon: ImageVector, title: String, hint: String, onClick: () -> Unit, modifier: Modifier) {
    SectionCard(onClick = onClick, modifier = modifier.fillMaxHeight()) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(Sizes.icon))
        Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 2)
        Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** 하단 링크가 부르는 동작. [privacyOptions]는 동의 필요 지역에서만 있다. */
private class FooterActions(
    val openUsageGuide: () -> Unit,
    val openExternal: (String) -> Unit,
    val openLicenses: () -> Unit,
    val privacyOptions: (() -> Unit)?
)

/**
 * 메인 하단 법적 진입점(`design/specs/main-legal-footer.md`): 저작권 안내 한 줄, 링크 묶음, 버전.
 * 아래 여백은 바깥 스크롤의 `Spacing.xl`이 맡는다(배너와 24dp 이상, 잘못 누르기 방지).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Footer(versionName: String?, actions: FooterActions) {
    // 바깥 Column의 spacedBy(lg)에 더해 바로가기 카드와 Spacing.xl만큼 띄운다.
    Column(
        Modifier.fillMaxWidth().padding(top = Spacing.xl - Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        Text(stringResource(R.string.main_rights_notice), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, maxLines = 2)
        val links = LegalFooterPolicy.links(consentOptionsAvailable = actions.privacyOptions != null)
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            itemVerticalAlignment = Alignment.CenterVertically
        ) {
            links.forEachIndexed { index, link ->
                if (index > 0) Text(stringResource(R.string.separator_dot), style = MaterialTheme.typography.labelLarge,
                    color = BrandColors.Separator, modifier = Modifier.clearAndSetSemantics {})
                FooterLink(link, actions)
            }
        }
        LegalFooterPolicy.versionLabel(versionName)?.let { version ->
            Text(stringResource(R.string.main_version, version), color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun FooterLink(link: LegalLink, actions: FooterActions) {
    val url = link.url?.let { stringResource(it) }
    val onClick: () -> Unit = when (link) {
        LegalLink.USAGE_GUIDE -> actions.openUsageGuide
        LegalLink.PRIVACY_POLICY -> { { url?.let(actions.openExternal) } }
        LegalLink.OPEN_SOURCE_LICENSES -> actions.openLicenses
        LegalLink.AD_PRIVACY -> actions.privacyOptions ?: return
    }
    TextAction(
        text = stringResource(link.label),
        onClick = onClick,
        tone = TextActionTone.MUTED,
        trailingIcon = if (link.external) ImageVector.vectorResource(R.drawable.ic_open_in_new) else null,
        trailingIconDescription = if (link.external) stringResource(R.string.cd_opens_in_browser) else null
    )
}
