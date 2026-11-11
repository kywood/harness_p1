package com.example.torrentstreamplayer.ui

import android.content.Context
import com.example.torrentstreamplayer.engine.InvalidMagnetException
import com.example.torrentstreamplayer.engine.TorrentInputResolver
import com.example.torrentstreamplayer.meta.TorrentFailure

/**
 * 링크 처리 실패를 사용자 문구로 바꾼다. 형식 오류는 영어·원문 링크가 담긴 예외 메시지 대신
 * [TorrentFailure]에 등록된 문구를 쓴다.
 */
internal object InputFailureText {
    /** [error]에 대응하는 등록된 실패. 예외 메시지가 이미 사용자 문구이면 null이다. */
    fun failure(error: Throwable): TorrentFailure? = when (error) {
        is InvalidMagnetException -> TorrentFailure.INVALID_MAGNET
        is TorrentInputResolver.InvalidTorrentFileException -> TorrentFailure.METADATA_DECODE
        else -> null
    }

    fun message(error: Throwable, context: Context): String? =
        failure(error)?.text(context) ?: error.localizedMessage
}
