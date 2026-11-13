package com.example.torrentstreamplayer.meta

import android.content.Context
import androidx.annotation.StringRes
import com.example.torrentstreamplayer.R

/**
 * Failures the torrent engine reports to the user. Register a new failure here with its
 * string resource; the engine passes the resolved text to its error callback.
 */
internal enum class TorrentFailure(@StringRes val message: Int) {
    INVALID_MAGNET(R.string.failure_invalid_magnet),
    METADATA_TIMEOUT(R.string.failure_metadata_timeout),
    METADATA_DECODE(R.string.failure_metadata_decode),
    NO_VIDEO(R.string.failure_no_video),
    DOWNLOAD_SETUP(R.string.failure_download_setup),
    ENGINE(R.string.failure_engine),

    /** A link could not be opened, e.g. the `.torrent` download failed on the network. */
    LINK_GENERIC(R.string.error_link_generic),

    // 입력 주소 확인과 .torrent 내려받기(TorrentInputResolver)
    INPUT_LENGTH(R.string.failure_input_length),
    INPUT_UNSUPPORTED(R.string.failure_input_unsupported),
    INPUT_HTTPS(R.string.failure_input_https),
    NOT_TORRENT(R.string.failure_not_torrent),
    REDIRECT_MISSING(R.string.failure_redirect_missing),
    TOO_MANY_REDIRECTS(R.string.failure_too_many_redirects),

    /** 인자 하나: HTTP 상태 코드. */
    TORRENT_HTTP(R.string.failure_torrent_http),
    TORRENT_TOO_LARGE(R.string.failure_torrent_too_large),

    // 다운로드 시작
    ALREADY_DOWNLOADING(R.string.failure_already_downloading),
    TASK_UNAVAILABLE(R.string.failure_task_unavailable),

    // 삭제(TorrentRemoval, ActiveTorrentFiles, 종료 대기)
    FILE_IN_USE(R.string.failure_file_in_use),
    ITEM_IN_USE(R.string.failure_item_in_use),
    STOP_PENDING(R.string.failure_stop_pending),
    RECORD_PENDING(R.string.failure_record_pending),
    STOP_DOWNLOADS_FIRST(R.string.failure_stop_downloads_first),
    ITEM_PATH(R.string.failure_item_path),
    OUTSIDE_STORAGE(R.string.failure_outside_storage),
    FOLDER_DELETE(R.string.failure_folder_delete),
    SYMLINK_DELETE(R.string.failure_symlink_delete);

    /** 등록된 문구만 돌려준다. 예외 원문은 사용자에게 보이지 않게 받지 않는다. [args]는 문구의 포맷 인자다. */
    fun text(context: Context, vararg args: Any): String = context.getString(message, *args)

    /** `require` 대신 던지는 실패(입력이 잘못됨). */
    fun invalid(cause: Throwable? = null): IllegalArgumentException = InvalidInputFailure(this, cause)

    /** `check`·`error` 대신 던지는 실패(지금 상태로는 할 수 없음). [args]는 문구의 포맷 인자다. */
    fun blocked(vararg args: Any): IllegalStateException = BlockedFailure(this, args.toList())
}

/**
 * 사용자에게 [failure] 문구로 보일 실패. 예외 메시지에는 문구 대신 항목 이름만 담아, 문구 언어가 코드에 들어가지 않고
 * 로그에 원문이 남지 않는다. 화면은 `InputFailureText`로 문구를 얻는다.
 */
internal sealed interface UserFacingFailure {
    val failure: TorrentFailure
    val args: List<Any>

    fun text(context: Context): String = failure.text(context, *args.toTypedArray())
}

private class InvalidInputFailure(override val failure: TorrentFailure, cause: Throwable?) :
    IllegalArgumentException(failure.name, cause), UserFacingFailure {
    override val args: List<Any> get() = emptyList()
}

private class BlockedFailure(override val failure: TorrentFailure, override val args: List<Any>) :
    IllegalStateException(failure.name), UserFacingFailure
