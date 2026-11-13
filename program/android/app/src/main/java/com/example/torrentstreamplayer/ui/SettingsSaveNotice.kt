package com.example.torrentstreamplayer.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * "설정을 저장하지 못했습니다" 스낵바를 한 번만 보이게 하는 상태. [MainViewModel]이 저장 실패 때 [report]하고, 설정
 * 화면이 보인 뒤 [consume]한다. 설정 화면을 열거나 나가면 [clear]해서, 지난 실패가 다음에 설정을 열 때 뜨지 않게 한다.
 */
internal class SettingsSaveNotice {
    private val pending = MutableStateFlow(false)

    /** true이면 설정 화면이 스낵바를 띄운다. */
    val failed: StateFlow<Boolean> = pending.asStateFlow()

    fun report() {
        pending.value = true
    }

    /** 스낵바를 띄웠다. */
    fun consume() {
        pending.value = false
    }

    /** 설정 화면을 열거나 나갔다: 보이지 못한 실패는 버린다. */
    fun clear() {
        pending.value = false
    }
}
