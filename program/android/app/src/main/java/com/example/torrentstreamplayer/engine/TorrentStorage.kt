package com.example.torrentstreamplayer.engine

import android.content.Context
import android.util.Log
import com.example.torrentstreamplayer.meta.TorrentStorageDir
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption

/**
 * Where the engine keeps downloads and state.
 *
 * Files live under `noBackupFilesDir`: unlike `cacheDir` they are not removed when the system
 * runs low on space or when the user clears the app cache, and they are excluded from Auto Backup.
 * Earlier versions stored them under `cacheDir`; [migrateLegacy] moves those once.
 */
internal class TorrentStorage(private val root: File, private val legacyRoot: File) {

    fun dir(kind: TorrentStorageDir): File = File(root, kind.dirName)

    /** Moves every legacy directory into [root]. Never overwrites a file that already exists there. */
    fun migrateLegacy(): MigrationResult = TorrentStorageDir.entries.fold(MigrationResult()) { total, kind ->
        total + move(File(legacyRoot, kind.dirName), dir(kind))
    }

    private fun move(source: File, target: File): MigrationResult {
        if (!exists(source) || isLink(source)) return MigrationResult()
        if (!exists(target)) {
            target.parentFile?.mkdirs()
            if (source.renameTo(target)) return MigrationResult(moved = 1)
        }
        if (!source.isDirectory || !target.isDirectory) return MigrationResult(kept = 1)
        val result = source.listFiles().orEmpty().fold(MigrationResult()) { total, child ->
            total + move(child, File(target, child.name))
        }
        source.delete() // Removes the legacy folder only when everything inside moved.
        return result
    }

    private fun exists(file: File) = Files.exists(file.toPath(), LinkOption.NOFOLLOW_LINKS)
    private fun isLink(file: File) = Files.isSymbolicLink(file.toPath())

    data class MigrationResult(val moved: Int = 0, val kept: Int = 0) {
        operator fun plus(other: MigrationResult) = MigrationResult(moved + other.moved, kept + other.kept)
    }

    companion object {
        private const val TAG = "TorrentStorage"
        @Volatile private var migrated = false

        /** Storage for this app; migrates legacy cache files the first time it is called in a process. */
        fun of(context: Context): TorrentStorage {
            val storage = TorrentStorage(context.noBackupFilesDir, context.cacheDir)
            if (!migrated) synchronized(this) {
                if (!migrated) {
                    runCatching { storage.migrateLegacy() }
                        .onSuccess { if (it.kept > 0) Log.w(TAG, "Kept ${it.kept} legacy entries that already exist") }
                        .onFailure { Log.e(TAG, "Legacy storage migration failed: ${EngineFailureLog.describe(it)}") }
                    migrated = true
                }
            }
            return storage
        }
    }
}
