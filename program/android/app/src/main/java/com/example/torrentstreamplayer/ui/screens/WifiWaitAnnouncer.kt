package com.example.torrentstreamplayer.ui.screens

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import com.example.torrentstreamplayer.R
import com.example.torrentstreamplayer.ui.WifiWaitPolicy
import kotlinx.coroutines.launch

/**
 * 받기를 시작한 뒤 부르는 함수 하나를 돌려준다(`design/specs/settings-wifi.md` 4절). 지금 새 다운로드가 Wi-Fi 대기로
 * 멈추면([waitsForWifi], [WifiWaitPolicy]) [snackbar]에 `info_waiting_for_wifi`를 띄운다. 재생 목록 카드·선택
 * 다운로드와 링크 추가 창이 모두 이것을 거쳐 어디서 받기를 시작하든 같게 동작한다.
 */
@Composable
internal fun rememberWifiWaitAnnouncer(snackbar: SnackbarHostState, waitsForWifi: Boolean): () -> Unit {
    val scope = rememberCoroutineScope()
    val message = stringResource(R.string.info_waiting_for_wifi)
    val waiting by rememberUpdatedState(waitsForWifi)
    return { if (waiting) scope.launch { snackbar.showSnackbar(message) } }
}
