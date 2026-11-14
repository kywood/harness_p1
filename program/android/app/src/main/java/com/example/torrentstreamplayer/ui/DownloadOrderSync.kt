package com.example.torrentstreamplayer.ui

import com.example.torrentstreamplayer.engine.DownloadOrderPreference
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * 받는 순서를 다시 계산해 작업에 넘기는 일을 한 번에 하나씩 한다. 설정 기본값 변경과 영상별 값 변경이 겹쳐도,
 * 먼저 계산한 오래된 값이 나중에 적용되어 새 값을 덮지 않는다(계산과 적용이 [lock] 안에서 한 묶음이다).
 *
 * @param lookup 작업의 받는 순서를 찾는다([io]에서 부른다).
 * @param apply 계산한 순서를 작업에 넘긴다. [sync]를 부른 쪽의 스레드(메인)에서 불린다.
 */
internal class DownloadOrderSync(
    private val lookup: DownloadOrderLookup,
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val apply: (key: String, order: DownloadOrderPreference) -> Unit,
) {
    private val lock = Mutex()

    /**
     * [save]가 있으면 먼저 저장하고(실패하면 예외를 던지고 아무것도 적용하지 않는다), [keys]의 작업마다 지금 순서를
     * 계산해 적용한다.
     *
     * @param keys 적용할 작업의 마그넷 키. 차례가 왔을 때 부른다.
     */
    suspend fun sync(keys: () -> Collection<String>, save: () -> Unit = {}) = lock.withLock {
        val targets = keys()
        val orders = withContext(io) {
            save()
            lookup.ordersFor(targets)
        }
        orders.forEach { (key, order) -> apply(key, order) }
    }
}
