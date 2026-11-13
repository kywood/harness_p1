package com.example.torrentstreamplayer.ui

/**
 * 재생 목록 맨 위 요약 줄의 숫자: "다운로드 중 N개 · 대기 N개 · Wi-Fi 대기 N개". 0개인 대기·Wi-Fi 대기 조각은 뺀다
 * (`design/specs/settings-wifi.md` 4절). 다운로드 중 조각은 0개여도 늘 보인다.
 */
internal data class PlaylistSummary(val downloading: Int, val queued: Int, val waitingForWifi: Int) {
    val showsQueued: Boolean get() = queued > 0
    val showsWifiWaiting: Boolean get() = waitingForWifi > 0

    /** 살아 있는 작업이 있어 목록을 주기적으로 다시 읽고 "모두 중지"를 보인다. */
    val hasLiveJobs: Boolean get() = downloading > 0 || queued > 0 || waitingForWifi > 0

    companion object {
        /** [downloading]은 화면 밖에서 센 받는 중 개수(재생에 빌린 작업 포함)를 그대로 받는다. */
        fun of(jobs: Collection<DownloadEntry>, downloading: Int): PlaylistSummary = PlaylistSummary(
            downloading = downloading.coerceAtLeast(0),
            queued = jobs.count { it.phase == DownloadPhase.QUEUED },
            waitingForWifi = jobs.count { it.phase == DownloadPhase.WAITING_FOR_WIFI },
        )
    }
}
