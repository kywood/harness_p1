package com.example.torrentstreamplayer.data

import android.content.Context
import com.example.torrentstreamplayer.engine.TorrentCache
import com.example.torrentstreamplayer.engine.TorrentCacheCipher
import java.io.File

/**
 * Encrypted app-wide state (not tied to one torrent) under `noBackupFilesDir/<name>`: the playlist and the settings.
 * Changing a name or [APP_KEY] makes the saved file unreadable.
 */
internal object AppStateCache {
    /** Cache key for app-wide state. [TorrentCache] wants a 64-digit key; torrents use their magnet key. */
    private val APP_KEY = "0".repeat(64)

    fun create(context: Context, name: String): TorrentCache =
        open(context.noBackupFilesDir, name, TorrentCacheCipher::encrypt, TorrentCacheCipher::decrypt)

    /** [create] without Android: [root] stands for `noBackupFilesDir`. */
    fun open(root: File, name: String, encrypt: (ByteArray) -> ByteArray, decrypt: (ByteArray) -> ByteArray) =
        TorrentCache(File(root, name), APP_KEY, encrypt, decrypt)
}
