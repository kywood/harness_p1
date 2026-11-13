package com.example.torrentstreamplayer.ui

import com.example.torrentstreamplayer.data.ConnectionType

/**
 * 지금 새 다운로드가 "Wi-Fi 대기"로 멈추는지(`design/specs/settings-wifi.md` 2·4·7절). 설정 화면 안내 줄과 재생 목록
 * 다운로드 버튼 스낵바(`info_waiting_for_wifi`)가 같은 판단을 쓴다.
 */
internal object WifiWaitPolicy {
    /**
     * Wi-Fi 전용이 켜져 있고 지금 연결이 데이터 제한 연결일 때만 true. 연결이 아예 없으면 멈춘 이유가 Wi-Fi 전용이
     * 아니므로 false. 판단은 다운로드 규칙과 같은 [DownloadNetworkPolicy.decide]를 쓴다.
     */
    fun waitsForWifi(wifiOnly: Boolean, connection: ConnectionType): Boolean =
        connection.connected &&
            DownloadNetworkPolicy.decide(NetworkConditions(wifiOnly, connection), allowedOnce = false) ==
            NetworkDecision.WAIT_FOR_WIFI
}
