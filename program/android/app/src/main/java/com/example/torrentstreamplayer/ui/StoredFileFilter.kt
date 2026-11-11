package com.example.torrentstreamplayer.ui
import com.example.torrentstreamplayer.engine.StoredTorrentFile

internal object StoredFileFilter {
    fun extension(name: String): String = if (name.lastIndexOf('.') <= 0) "" else
        name.substringAfterLast('.').lowercase(java.util.Locale.ROOT)
    fun visible(files: List<StoredTorrentFile>, selectedExtension: String?): List<StoredTorrentFile> =
        files.filter { selectedExtension == null || extension(it.name) == selectedExtension }
}
