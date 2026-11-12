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
    LINK_GENERIC(R.string.error_link_generic);

    /** 등록된 문구만 돌려준다. 예외 원문은 사용자에게 보이지 않게 받지 않는다. */
    fun text(context: Context): String = context.getString(message)
}
