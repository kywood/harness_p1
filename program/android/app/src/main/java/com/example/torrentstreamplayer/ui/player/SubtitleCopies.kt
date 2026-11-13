package com.example.torrentstreamplayer.ui.player

import android.util.Log
import com.example.torrentstreamplayer.engine.DownloadPathGuard
import com.example.torrentstreamplayer.engine.EngineFailureLog
import com.example.torrentstreamplayer.meta.PlayerSpec
import com.example.torrentstreamplayer.ui.SubtitleEncoding
import com.example.torrentstreamplayer.ui.SubtitleEncodingPolicy
import java.io.File

/**
 * UTF-8 copies of CP949 subtitle files for one playback ([SubtitleEncodingPolicy]), kept in [root] under the app
 * cache, outside the download folder. Other files go to the player as they are. Runs on the engine command queue,
 * like the session that calls it. [clear] removes the copies when playback ends (and leftovers on start).
 */
internal class SubtitleCopies(private val root: File) {
    private var count = 0

    /** The file to hand to the player for [file]: a UTF-8 copy for a Korean CP949 file, otherwise [file] itself. */
    fun prepare(file: File): File = runCatching {
        if (!file.isFile || file.length() > PlayerSpec.SUBTITLE_COPY_MAX_BYTES) return file
        val bytes = file.readBytes()
        if (SubtitleEncodingPolicy.detect(bytes) != SubtitleEncoding.CP949) return file
        val text = SubtitleEncodingPolicy.decodeCp949(bytes) ?: return file
        root.mkdirs()
        // A numbered name keeps two subtitles with the same name (Subs/E01/Korean.smi, Subs/E02/Korean.smi) apart.
        val copy = DownloadPathGuard.resolve(root, "${count++}_${file.name}") ?: return file
        copy.writeText(BYTE_ORDER_MARK + text, Charsets.UTF_8)
        copy
    }.getOrElse { failure ->
        Log.w(TAG, "Subtitle copy failed: ${EngineFailureLog.describe(failure)}")
        file
    }

    fun clear() {
        runCatching { root.deleteRecursively() }
    }

    private companion object {
        const val TAG = "SubtitleCopies"

        /** Marks the copy as UTF-8 for the player. */
        const val BYTE_ORDER_MARK = "﻿"
    }
}
