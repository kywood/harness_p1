package com.example.torrentstreamplayer.ui

import com.example.torrentstreamplayer.engine.DownloadOrderPreference

/**
 * 받기 전용 작업이 쓸 받는 순서를 찾는다: 영상별 값이 있으면 그 값, 없으면 설정의 기본값
 * ([DownloadOrderPreference.effective]). [perVideo]는 디스크를 읽으므로 메인 스레드가 아닌 곳에서 부른다.
 *
 * @param perVideo 영상별로 정한 값(마그넷 키 → 값). 읽지 못하면 영상별 값이 없는 것으로 본다.
 * @param appDefault 설정의 기본값. 부를 때마다 지금 값을 돌려준다.
 */
internal class DownloadOrderLookup(
    private val perVideo: () -> Map<String, DownloadOrderPreference>,
    private val appDefault: () -> DownloadOrderPreference,
) {
    /** 작업 하나의 받는 순서. */
    fun orderFor(key: String): DownloadOrderPreference = ordersFor(listOf(key)).getValue(key)

    /** 작업 여러 개의 받는 순서. 영상별 값은 한 번만 읽는다. */
    fun ordersFor(keys: Collection<String>): Map<String, DownloadOrderPreference> {
        if (keys.isEmpty()) return emptyMap()
        val saved = runCatching(perVideo).getOrDefault(emptyMap())
        val fallback = appDefault()
        return keys.associateWith { DownloadOrderPreference.effective(saved[it], fallback) }
    }
}
