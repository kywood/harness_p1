package com.example.torrentstreamplayer.ui

import android.content.Context
import com.example.torrentstreamplayer.engine.InvalidMagnetException
import com.example.torrentstreamplayer.engine.TorrentInputResolver
import com.example.torrentstreamplayer.meta.TorrentFailure
import com.example.torrentstreamplayer.meta.UserFacingFailure
import java.io.IOException

/**
 * 링크 처리 실패를 사용자 문구로 바꾼다. 형식 오류와 네트워크 오류는 영어·원문 링크·호스트가 담긴 예외 메시지 대신
 * [TorrentFailure]에 등록된 문구를 쓴다. 앱이 직접 던진 실패는 [UserFacingFailure]에 담긴 문구를 쓴다.
 * 예외 메시지 원문은 사용자에게 보이지 않는다.
 */
internal object InputFailureText {
    /** [error]에 대응하는 등록된 실패. 등록된 실패가 없으면 null이다. */
    fun failure(error: Throwable): TorrentFailure? = when (error) {
        is UserFacingFailure -> error.failure
        is InvalidMagnetException -> TorrentFailure.INVALID_MAGNET
        is TorrentInputResolver.InvalidTorrentFileException -> TorrentFailure.METADATA_DECODE
        // UnknownHost, SSL, 시간 초과, MalformedURL 모두 IOException이다. 메시지에 호스트·영어 원문이 들어 있다.
        is IOException -> TorrentFailure.LINK_GENERIC
        else -> null
    }

    /** 앱이 [UserFacingFailure]로 직접 던진 실패의 문구. 그 밖의 예외는 null이다(삭제 실패처럼 링크와 무관한 곳용). */
    fun ownMessage(error: Throwable, context: Context): String? = (error as? UserFacingFailure)?.text(context)

    fun message(error: Throwable, context: Context): String? =
        ownMessage(error, context) ?: failure(error)?.text(context)
}
