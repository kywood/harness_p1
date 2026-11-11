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
    ENGINE(R.string.failure_engine);

    fun text(context: Context, detail: String? = null): String =
        if (this == ENGINE) context.getString(message, detail ?: context.getString(R.string.failure_unknown))
        else context.getString(message)
}
