package com.example.torrentstreamplayer.ui

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 대기 작업의 재생 목록 기록과 재생 목록 삭제의 순서를 맞춘다(#148).
 *
 * 기록은 차례가 왔을 때 작업이 아직 살아 있을 때만 한다([alive]). 삭제는 작업을 멈춘 뒤 지우므로, 삭제가 먼저
 * 잠금을 잡으면 뒤에 온 기록은 건너뛰고, 기록이 먼저면 삭제가 그 뒤에 지운다. 지운 카드가 늦은 기록으로 다시
 * 나타나지 않는다.
 *
 * @param alive 작업이 아직 살아 있는지. [DownloadManager]처럼 메인 스레드 전용이면 [recordIfAlive]를 메인에서 부른다.
 */
internal class PlaylistWriteGate(private val alive: (String) -> Boolean) {
    private val lock = Mutex()

    /** [key]가 아직 살아 있으면 [write]를 하고 true. 이미 멈췄으면(삭제·중지) 쓰지 않고 false. */
    suspend fun recordIfAlive(key: String, write: suspend () -> Unit): Boolean = lock.withLock {
        if (!alive(key)) return@withLock false
        write()
        true
    }

    /** 작업 중지와 삭제를 [block] 하나로 묶어, 그 사이에 기록이 끼어들지 않게 한다. */
    suspend fun <T> delete(block: suspend () -> T): T = lock.withLock { block() }
}
