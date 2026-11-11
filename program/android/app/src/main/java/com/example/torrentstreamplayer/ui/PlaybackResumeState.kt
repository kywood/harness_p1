package com.example.torrentstreamplayer.ui

import com.example.torrentstreamplayer.engine.PlaybackResume

/**
 * 재생 중인 항목과 다시 시작할 위치. 재생 화면이 다시 만들어지면(테마·글꼴 크기 변경 등으로 Activity 재생성)
 * 처음 연 위치가 아니라 마지막으로 저장한 위치에서 이어 간다.
 */
internal class PlaybackResumeState {
    /** 재생 중인 항목의 마그넷. 재생을 시작하기 전에는 null이다. */
    var magnet: String? = null
        private set

    /** 재생 화면을 열 때 이동할 위치(ms). */
    var positionMs: Long = 0
        private set

    fun start(magnet: String, positionMs: Long) {
        this.magnet = magnet
        this.positionMs = positionMs.coerceAtLeast(0)
    }

    /** [magnet]의 위치가 저장될 때 부른다. 다른 항목의 늦은 저장은 무시한다. */
    fun saved(magnet: String, positionMs: Long, durationMs: Long) {
        if (magnet != this.magnet) return
        this.positionMs = PlaybackResume.position(positionMs.coerceAtLeast(0), durationMs.coerceAtLeast(0))
    }
}
