package com.example.torrentstreamplayer.ui.player

import android.util.Log
import com.example.torrentstreamplayer.engine.DownloadPathGuard
import com.example.torrentstreamplayer.engine.EngineFailureLog
import com.example.torrentstreamplayer.meta.PlayerSpec
import com.example.torrentstreamplayer.ui.SubtitleEncoding
import com.example.torrentstreamplayer.ui.SubtitleEncodingPolicy
import java.io.File
import java.util.UUID

/**
 * UTF-8 copies of CP949 subtitle files for one playback ([SubtitleEncodingPolicy]), kept in a folder of its own,
 * `[parent]/<id>/`, under the app cache and outside the download folder. Other files go to the player as they are.
 *
 * Every playback has a new [id], so the previous playback's [clear] or a late [prepare] works only in its own folder
 * and never removes the next video's copies (PR #153 review). The first [prepare] removes the older folders under
 * [parent] (named `<start time>-<UUID>`): leftovers of earlier playbacks, including ones a crash left behind. A newer
 * folder is never removed, so a late first [prepare] of the previous playback cannot remove the next one's.
 *
 * [prepare] reads and writes files, so it runs on the IO dispatcher, not on the engine command queue; [copyOf] only
 * looks the result up, so the subtitle session can call it on the queue without waiting for any file work. File work
 * holds [ioLock] ([clear] removes the folder only after a running [prepare] ends); the copy table holds `this` (after
 * [clear] a finishing [prepare] deletes its copy instead of publishing it).
 */
internal class SubtitleCopies(
    private val parent: File,
    private val startedAtMs: Long = System.currentTimeMillis(),
    private val id: String = "$startedAtMs-${UUID.randomUUID()}",
) {
    /** This playback's folder, or null when [id] would not name a folder directly inside [parent]. */
    private val root: File? = DownloadPathGuard.resolve(parent, id)
        ?.takeIf { it.parentFile?.canonicalFile == parent.canonicalFile }
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
                removeOtherPlaybacks()
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

    /** Removes this playback's copies; later [prepare] calls do nothing. Call on the IO dispatcher. */
    fun clear() {
        synchronized(this) {
            closed = true
            copies.clear()
        }
        synchronized(ioLock) { runCatching { root?.deleteRecursively() } }
    }

    /** Folders under [parent] started before this playback (or not named by one). */
    private fun removeOtherPlaybacks() {
        runCatching {
            parent.listFiles().orEmpty()
                .filter { it.name != id && (it.name.substringBefore('-').toLongOrNull() ?: 0L) <= startedAtMs }
                .forEach { it.deleteRecursively() }
        }
    }

    private fun copy(file: File): File? = runCatching {
        val folder = root ?: return null
        if (!file.isFile || file.length() > PlayerSpec.SUBTITLE_COPY_MAX_BYTES) return null
        val bytes = file.readBytes()
        if (SubtitleEncodingPolicy.detect(bytes) != SubtitleEncoding.CP949) return null
        val text = SubtitleEncodingPolicy.decodeCp949(bytes) ?: return null
        folder.mkdirs()
        // A numbered name keeps two subtitles with the same name (Subs/E01/Korean.smi, Subs/E02/Korean.smi) apart.
        val copy = DownloadPathGuard.resolve(folder, "${count++}_${file.name}") ?: return null
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
