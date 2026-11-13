package com.example.torrentstreamplayer.ui.screens

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.example.torrentstreamplayer.R
import com.example.torrentstreamplayer.ui.LegalLink
import com.example.torrentstreamplayer.ui.LegalLinkActions
import com.example.torrentstreamplayer.ui.components.rememberExternalLinkOpener
import kotlinx.coroutines.launch

/**
 * 법적 링크 연결 하나(메인 하단, 설정 "정보" 묶음이 같이 쓴다). 이용 안내 시트를 열고 닫는 상태와 시트 자체, 외부
 * 브라우저 열기와 실패 안내(`error_browser_unavailable`를 [snackbar]에)를 여기서 맡는다. 시트는 이 함수를 부른 자리에
 * 그려진다.
 *
 * @param onPrivacyOptions 광고 개인정보 설정. 동의가 필요한 지역에서만 넘긴다.
 */
@Composable
internal fun rememberLegalLinkActions(
    snackbar: SnackbarHostState,
    onOpenLicenses: () -> Unit,
    onPrivacyOptions: (() -> Unit)?
): LegalLinkActions {
    val scope = rememberCoroutineScope()
    val openExternal = rememberExternalLinkOpener()
    val browserUnavailable = stringResource(R.string.error_browser_unavailable)
    var showUsageGuide by rememberSaveable { mutableStateOf(false) }
    if (showUsageGuide) UsageGuideSheet(onDismiss = { showUsageGuide = false }, openExternal = openExternal)
    return LegalLinkActions(
        openUsageGuide = { showUsageGuide = true },
        openExternal = { url -> if (!openExternal(url)) scope.launch { snackbar.showSnackbar(browserUnavailable) } },
        openLicenses = onOpenLicenses,
        privacyOptions = onPrivacyOptions
    )
}

/** [link]를 눌렀을 때의 동작. 주소 리소스를 읽어 [LegalLinkActions.actionFor]에 넘긴다. 동작이 없으면 null. */
@Composable
internal fun LegalLinkActions.onClickFor(link: LegalLink): (() -> Unit)? =
    actionFor(link, link.url?.let { stringResource(it) })
