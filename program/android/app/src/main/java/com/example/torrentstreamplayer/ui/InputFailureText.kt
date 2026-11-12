package com.example.torrentstreamplayer.ui

import android.content.Context
import com.example.torrentstreamplayer.engine.InvalidMagnetException
import com.example.torrentstreamplayer.engine.TorrentInputResolver
import com.example.torrentstreamplayer.meta.TorrentFailure
import java.io.IOException

/**
 * 링크 처리 실패를 사용자 문구로 바꾼다. 형식 오류와 네트워크 오류는 영어·원문 링크·호스트가 담긴 예외 메시지 대신
 * [TorrentFailure]에 등록된 문구를 쓴다. 예외 메시지는 앱이 직접 던진 한국어 문구([ownMessage])일 때만 보인다.
 */
internal object InputFailureText {
    private val HANGUL = Regex("[가-힣]")

    /** [error]에 대응하는 등록된 실패. 등록된 실패가 없으면 null이다. */
    fun failure(error: Throwable): TorrentFailure? = when (error) {
        is InvalidMagnetException -> TorrentFailure.INVALID_MAGNET
        is TorrentInputResolver.InvalidTorrentFileException -> TorrentFailure.METADATA_DECODE
        // UnknownHost, SSL, 시간 초과, MalformedURL 모두 IOException이다. 메시지에 호스트·영어 원문이 들어 있다.
        is IOException -> TorrentFailure.LINK_GENERIC
        else -> null
    }

    /** 앱이 `require`/`check`로 직접 던진 한국어 메시지. 라이브러리 예외의 원문은 null이다. */
    fun ownMessage(error: Throwable): String? = error.message?.takeIf {
        (error is IllegalArgumentException || error is IllegalStateException) && HANGUL.containsMatchIn(it)
    }

    fun message(error: Throwable, context: Context): String? =
        failure(error)?.text(context) ?: ownMessage(error)
}
