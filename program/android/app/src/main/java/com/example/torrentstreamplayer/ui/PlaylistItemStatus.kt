package com.example.torrentstreamplayer.ui

import androidx.annotation.StringRes
import com.example.torrentstreamplayer.R
import com.example.torrentstreamplayer.ui.components.StatusTone

/** 재생 목록 항목의 상태 칩. 종류마다 문구와 색 종류를 등록한다. */
internal enum class PlaylistItemStatus(@StringRes val label: Int, val tone: StatusTone) {
    DOWNLOADING(R.string.status_downloading, StatusTone.DOWNLOADING),
    QUEUED(R.string.status_queued, StatusTone.QUEUED),
    FAILED(R.string.status_failed, StatusTone.FAILED),
    COMPLETED(R.string.label_complete, StatusTone.COMPLETED),
    INCOMPLETE(R.string.label_incomplete, StatusTone.NEUTRAL);

    companion object {
        /** 받는 중·대기가 실패 기록보다 앞선다(다시 받기 시작하면 실패 표시를 지운다). */
        fun of(active: Boolean, queued: Boolean, failed: Boolean, complete: Boolean): PlaylistItemStatus = when {
            queued -> QUEUED
            active && !complete -> DOWNLOADING
            complete -> COMPLETED
            failed -> FAILED
            else -> INCOMPLETE
        }
    }
}
