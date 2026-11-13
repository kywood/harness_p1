package com.example.torrentstreamplayer.meta

import androidx.annotation.StringRes
import com.example.torrentstreamplayer.R

/**
 * 준비 화면 세부 정보의 단계 줄. 엔진은 단계만 알리고 문구는 화면이 [label]로 읽는다.
 * [SEARCHING]의 문구는 지난 초(`MetadataDetails.elapsedSec`)를 인자로 받는다.
 */
internal enum class MetadataStage(@StringRes val label: Int) {
    RESOLVING(R.string.metadata_stage_resolving),
    PREPARING_SESSION(R.string.metadata_stage_session),
    CHECKING_SAVED(R.string.metadata_stage_saved),
    CONNECTING(R.string.metadata_stage_connecting),
    SEARCHING(R.string.metadata_stage_searching),
    RECEIVED(R.string.metadata_stage_received),
    STARTUP_PIECES(R.string.metadata_stage_startup),
    DOWNLOADING(R.string.metadata_stage_downloading),
    COMPLETE(R.string.metadata_stage_complete),
}

/** 메타데이터를 어디서 얻는지. 세부 정보의 "정보 출처" 값이다. */
internal enum class MetadataOrigin(@StringRes val label: Int) {
    NETWORK(R.string.metadata_origin_network),
    SAVED(R.string.metadata_origin_saved),
}
