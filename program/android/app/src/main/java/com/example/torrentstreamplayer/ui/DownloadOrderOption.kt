package com.example.torrentstreamplayer.ui

import androidx.annotation.StringRes
import com.example.torrentstreamplayer.R
import com.example.torrentstreamplayer.engine.DownloadOrderPreference

/**
 * "받는 순서" 선택 창의 항목(`design/specs/download-order.md`). 항목마다 저장할 값과 문구를 등록한다.
 *
 * @param preference 저장할 값. null은 영상별 선택의 "기본 설정 따름"이다.
 * @param description 항목 아래 설명. 없으면 null.
 */
internal enum class DownloadOrderOption(
    val preference: DownloadOrderPreference?,
    @StringRes val label: Int,
    @StringRes val description: Int? = null,
) {
    USE_DEFAULT(null, R.string.download_order_use_default),
    FROM_START(DownloadOrderPreference.FROM_START, R.string.download_order_from_start,
        R.string.download_order_from_start_desc),
    FASTEST(DownloadOrderPreference.FASTEST, R.string.download_order_fastest, R.string.download_order_fastest_desc);

    companion object {
        /** 설정 화면의 선택지: 앱 전체 기본값이라 "기본 설정 따름"이 없다. */
        val forSettings: List<DownloadOrderOption> = entries.filter { it.preference != null }

        /** 재생 목록 카드의 선택지: "기본 설정 따름"이 맨 앞이다. */
        val forVideo: List<DownloadOrderOption> = entries

        /** 저장된 값의 항목. null(정하지 않음)은 [USE_DEFAULT]다. */
        fun of(preference: DownloadOrderPreference?): DownloadOrderOption = entries.first { it.preference == preference }
    }
}

/**
 * 재생 목록 카드의 "받는 순서" 줄. 다 받은 영상은 받을 것이 없어 줄 전체를 숨긴다.
 *
 * @param showsAction "받는 순서" 버튼을 보인다.
 * @param marker 영상별로 정한 값의 표시. "기본 설정 따름"이면 null(표시 없음).
 */
internal data class DownloadOrderCardState(val showsAction: Boolean, val marker: DownloadOrderOption?) {
    companion object {
        private val HIDDEN = DownloadOrderCardState(showsAction = false, marker = null)

        /**
         * @param saved 영상별로 저장된 값(없으면 null).
         * @param complete 다 받은 영상.
         */
        fun of(saved: DownloadOrderPreference?, complete: Boolean): DownloadOrderCardState =
            if (complete) HIDDEN
            else DownloadOrderCardState(showsAction = true, marker = saved?.let(DownloadOrderOption::of))
    }
}
