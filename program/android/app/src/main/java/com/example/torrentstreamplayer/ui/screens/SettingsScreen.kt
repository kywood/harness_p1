package com.example.torrentstreamplayer.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.res.stringResource
import com.example.torrentstreamplayer.BuildConfig
import com.example.torrentstreamplayer.R
import com.example.torrentstreamplayer.data.AppSettings
import com.example.torrentstreamplayer.ui.LegalFooterPolicy
import com.example.torrentstreamplayer.ui.LegalLinkActions
import com.example.torrentstreamplayer.ui.WifiWaitPolicy
import com.example.torrentstreamplayer.ui.components.AppTopBar
import com.example.torrentstreamplayer.ui.components.InlineNotice
import com.example.torrentstreamplayer.ui.components.SectionHeader
import com.example.torrentstreamplayer.ui.components.SettingsLinkItem
import com.example.torrentstreamplayer.ui.components.SettingsSwitchItem
import com.example.torrentstreamplayer.ui.components.rememberExternalLinkOpener
import com.example.torrentstreamplayer.ui.theme.Sizes
import com.example.torrentstreamplayer.ui.theme.Spacing
import kotlinx.coroutines.launch

/**
 * 설정 화면(`design/specs/settings-wifi.md` 2절): 다운로드(Wi-Fi 전용), 재생(자막 자동 표시), 정보(메인 하단 링크와
 * 같은 동작 + 버전). 광고는 넣지 않는다. 스위치 값은 [settings]가 정하고, 바꾸면 콜백으로 ViewModel에 저장을 맡긴다.
 *
 * @param waitsForWifi Wi-Fi 전용 + 모바일 데이터라 다운로드가 멈춘 상태([WifiWaitPolicy]). 켜지면 안내 줄을 보인다.
 * @param saveFailed 저장 실패를 알리는 값. true이면 스낵바를 한 번 보이고 [onSaveFailedShown]을 부른다.
 * @param onPrivacyOptions 광고 개인정보 설정(동의 필요 지역에서만 넘긴다).
 */
@Composable
internal fun SettingsScreen(
    settings: AppSettings,
    onWifiOnlyChange: (Boolean) -> Unit,
    onAutoSubtitlesChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onOpenLicenses: () -> Unit,
    onPrivacyOptions: (() -> Unit)?,
    waitsForWifi: Boolean = false,
    saveFailed: Boolean = false,
    onSaveFailedShown: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val openExternal = rememberExternalLinkOpener()
    val browserUnavailable = stringResource(R.string.error_browser_unavailable)
    val saveFailedText = stringResource(R.string.settings_save_failed)
    var showUsageGuide by rememberSaveable { mutableStateOf(false) }
    val linkActions = LegalLinkActions(
        openUsageGuide = { showUsageGuide = true },
        openExternal = { url -> if (!openExternal(url)) scope.launch { snackbar.showSnackbar(browserUnavailable) } },
        openLicenses = onOpenLicenses,
        privacyOptions = onPrivacyOptions
    )
    BackHandler(onBack = onBack)
    LaunchedEffect(saveFailed) {
        if (!saveFailed) return@LaunchedEffect
        onSaveFailedShown()
        snackbar.showSnackbar(saveFailedText)
    }
    if (showUsageGuide) UsageGuideSheet(onDismiss = { showUsageGuide = false }, openExternal = openExternal)

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            AppTopBar(stringResource(R.string.title_settings), onBack = onBack)
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Column(
                    Modifier
                        .widthIn(max = Sizes.contentMaxWidth)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding()
                        .padding(bottom = Spacing.lg)
                ) {
                    SectionHeader(stringResource(R.string.settings_section_download), top = Spacing.lg)
                    SettingsSwitchItem(
                        title = stringResource(R.string.settings_wifi_only),
                        description = stringResource(R.string.settings_wifi_only_desc),
                        checked = settings.wifiOnly,
                        onCheckedChange = onWifiOnlyChange
                    )
                    if (waitsForWifi) InlineNotice(stringResource(R.string.settings_wifi_paused_notice),
                        Modifier.padding(horizontal = Spacing.screenGutter))

                    SectionHeader(stringResource(R.string.settings_section_playback))
                    SettingsSwitchItem(
                        title = stringResource(R.string.settings_auto_subtitles),
                        description = stringResource(R.string.settings_auto_subtitles_desc),
                        checked = settings.autoSubtitles,
                        onCheckedChange = onAutoSubtitlesChange
                    )

                    SectionHeader(stringResource(R.string.settings_section_about))
                    linkActions.links.forEach { link ->
                        val onClick = linkActions.actionFor(link, link.url?.let { stringResource(it) }) ?: return@forEach
                        SettingsLinkItem(stringResource(link.label), external = link.external, onClick = onClick)
                    }
                    LegalFooterPolicy.versionLabel(BuildConfig.VERSION_NAME)?.let { version ->
                        SettingsLinkItem(stringResource(R.string.settings_version), value = version)
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding())
    }
}
