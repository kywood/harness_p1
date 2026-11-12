package com.example.torrentstreamplayer.ui

import com.example.torrentstreamplayer.data.ConnectionType

/** 설정 화면 안내 줄을 보일지 정한다(`design/specs/settings-wifi.md` 2절·7절). */
internal object SettingsNoticePolicy {
    /**
     * "모바일 데이터에 연결되어 다운로드를 멈췄습니다": Wi-Fi 전용이 켜져 있고 지금 연결이 데이터 제한 연결일 때만.
     * 연결이 아예 없으면 멈춘 이유가 Wi-Fi 전용이 아니므로 보이지 않는다. 판단은 다운로드 규칙과 같은 정책을 쓴다.
     */
    fun showsWifiPaused(wifiOnly: Boolean, connection: ConnectionType): Boolean =
        connection.connected &&
            DownloadNetworkPolicy.decide(NetworkConditions(wifiOnly, connection), allowedOnce = false) ==
            NetworkDecision.WAIT_FOR_WIFI
}
