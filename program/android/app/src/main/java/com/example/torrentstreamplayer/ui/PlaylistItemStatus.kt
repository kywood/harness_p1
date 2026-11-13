package com.example.torrentstreamplayer.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.torrentstreamplayer.R
import com.example.torrentstreamplayer.ui.components.StatusTone

/**
 * 재생 목록 항목의 상태 칩. 종류마다 문구·색 종류·아이콘(없으면 점)과 카드 동작을 등록한다.
 *
 * @param showsSpeed 받은 양 옆에 속도를 보인다. Wi-Fi 대기는 0 B/s를 보이지 않도록 숨긴다.
 */
internal enum class PlaylistItemStatus(
    @StringRes val label: Int,
    val tone: StatusTone,
    @DrawableRes val icon: Int? = null,
    val showsSpeed: Boolean = true,
) {
    WAITING_FOR_WIFI(R.string.status_wifi_waiting, StatusTone.WAITING_WIFI, icon = R.drawable.ic_wifi,
        showsSpeed = false),
    DOWNLOADING(R.string.status_downloading, StatusTone.DOWNLOADING),
    QUEUED(R.string.status_queued, StatusTone.QUEUED),
    FAILED(R.string.status_failed, StatusTone.FAILED),
    COMPLETED(R.string.label_complete, StatusTone.COMPLETED),
    INCOMPLETE(R.string.label_incomplete, StatusTone.NEUTRAL);

    companion object {
        /**
         * Wi-Fi 대기가 가장 앞선다(원래 다운로드 중이었든 대기였든 칩 하나만 "Wi-Fi 대기").
         * 받는 중·대기가 실패 기록보다 앞선다(다시 받기 시작하면 실패 표시를 지운다).
         */
        fun of(active: Boolean, queued: Boolean, failed: Boolean, complete: Boolean,
            waitingForWifi: Boolean = false): PlaylistItemStatus = when {
            waitingForWifi -> WAITING_FOR_WIFI
            queued -> QUEUED
            active && !complete -> DOWNLOADING
            complete -> COMPLETED
            failed -> FAILED
            else -> INCOMPLETE
        }
    }
}
