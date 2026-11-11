package com.example.torrentstreamplayer.meta

/**
 * Directories the torrent engine owns. Register a new engine directory here; storage setup,
 * legacy migration and tests all iterate over [entries], so callers need no changes.
 */
internal enum class TorrentStorageDir(val dirName: String) {
    /** Downloaded video files, one sub-folder per magnet key. */
    DOWNLOADS("torrent_downloads"),

    /** Encrypted metadata, resume data and progress records, one sub-folder per magnet key. */
    STATE("torrent_state"),
}
