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
 * cache, outside the download folder. Other files go to the player as they are.
 *
 * [prepare] reads and writes files, so it runs on the IO dispatcher, not on the engine command queue; [copyOf] only
 * looks the result up, so the subtitle session can call it on the queue without waiting for any file work. Two locks
 * keep the order the review asked for: file work holds [ioLock] (leftovers of an earlier playback go before the first
 * copy; [clear] removes the folder only after a running [prepare] ends), the copy table holds `this` (after [clear]
 * a finishing [prepare] deletes its copy instead of publishing it).
 */
internal class SubtitleCopies(private val root: File) {
    private val ioLock = Any()
    private val copies = mutableMapOf<File, File>()
    private var count = 0
    private var cleaned = false
    private var closed = false

    /**
     * Makes the UTF-8 copy of [file] when it is Korean CP949. Each file is read once; a file that needs no copy is
     * remembered as itself. Call on the IO dispatcher.
     */
    fun prepare(file: File) {
        synchronized(ioLock) {
            if (synchronized(this) { closed || file in copies }) return
            if (!cleaned) {
                runCatching { root.deleteRecursively() }
                cleaned = true
            }
            val copy = copy(file) ?: file
            val published = synchronized(this) {
                if (!closed) copies[file] = copy
                !closed
            }
            if (!published && copy != file) copy.delete()
        }
    }

    /** The file to hand to the player for [file]: its UTF-8 copy when [prepare] made one, otherwise [file]. */
    @Synchronized fun copyOf(file: File): File = copies[file] ?: file

    /** Removes the copies; later [prepare] calls do nothing. Call on the IO dispatcher. */
    fun clear() {
        synchronized(this) {
            closed = true
            copies.clear()
        }
        synchronized(ioLock) { runCatching { root.deleteRecursively() } }
    }

    private fun copy(file: File): File? = runCatching {
        if (!file.isFile || file.length() > PlayerSpec.SUBTITLE_COPY_MAX_BYTES) return null
        val bytes = file.readBytes()
        if (SubtitleEncodingPolicy.detect(bytes) != SubtitleEncoding.CP949) return null
        val text = SubtitleEncodingPolicy.decodeCp949(bytes) ?: return null
        root.mkdirs()
        // A numbered name keeps two subtitles with the same name (Subs/E01/Korean.smi, Subs/E02/Korean.smi) apart.
        val copy = DownloadPathGuard.resolve(root, "${count++}_${file.name}") ?: return null
        copy.writeText(BYTE_ORDER_MARK + text, Charsets.UTF_8)
        copy
    }.getOrElse { failure ->
        Log.w(TAG, "Subtitle copy failed: ${EngineFailureLog.describe(failure)}")
        null
    }

    private companion object {
        const val TAG = "SubtitleCopies"

        /** Marks the copy as UTF-8 for the player. */
        const val BYTE_ORDER_MARK = "﻿"
    }
}
