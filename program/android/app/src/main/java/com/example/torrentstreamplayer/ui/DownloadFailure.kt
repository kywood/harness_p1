package com.example.torrentstreamplayer.ui

import android.content.Context
import com.example.torrentstreamplayer.R
import com.example.torrentstreamplayer.meta.TorrentFailure
import com.example.torrentstreamplayer.meta.UserFacingFailure

/**
 * 다운로드 작업의 실패. 실패한 때의 문자열 대신 "어떤 실패인지"를 들고 있다가, 화면이 [text]로 그때의 언어 문구를 얻는다.
 * 예외 원문은 들고 있지 않는다.
 */
internal sealed interface DownloadFailure {
    fun text(context: Context): String

    /** 등록된 실패와 그 포맷 인자. */
    data class Registered(val failure: TorrentFailure, val args: List<Any> = emptyList()) : DownloadFailure {
        override fun text(context: Context): String = failure.text(context, *args.toTypedArray())
    }

    /** 엔진이 오류 콜백으로 이미 등록 문구를 알린 실패(엔진은 [TorrentFailure.text]로 만든 문구만 넘긴다). */
    data class Reported(val message: String) : DownloadFailure {
        override fun text(context: Context): String = message
    }

    /** 등록되지 않은 예외: 기본 문구 `error_download_start_plain`. */
    data object StartFailed : DownloadFailure {
        override fun text(context: Context): String = context.getString(R.string.error_download_start_plain)
    }

    companion object {
        /** 시작 중에 난 [error]의 실패. 앱이 던진 실패는 인자까지, 링크 형식·네트워크 오류는 등록된 항목으로 바꾼다. */
        fun of(error: Throwable): DownloadFailure =
            (error as? UserFacingFailure)?.let { Registered(it.failure, it.args) }
                ?: InputFailureText.failure(error)?.let { Registered(it) }
                ?: StartFailed
    }
}
