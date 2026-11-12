package com.example.torrentstreamplayer.engine

import com.example.torrentstreamplayer.meta.SubtitleFormat
import com.example.torrentstreamplayer.meta.SubtitleSpec
import java.util.Locale

/** One file of the torrent as metadata lists it. [offset] is its first byte in the whole torrent. */
internal data class TorrentFileEntry(val index: Int, val path: String, val size: Long, val offset: Long)

/** A subtitle file chosen to download with the video, and the pieces that hold it. */
internal data class SubtitleCandidate(
    val index: Int,
    val path: String,
    val size: Long,
    val language: SubtitleLanguage?,
    /** True when its name starts like the video's name (or the reverse), so it belongs to that video. */
    val matchesVideoName: Boolean,
    val pieces: IntRange,
)

/**
 * Chooses the subtitle files downloaded with the selected video. Pure: no libtorrent, no disk.
 *
 * A file qualifies when its extension is a [SubtitleFormat], its size is 1..[SubtitleSpec.MAX_FILE_BYTES],
 * it sits in the video's folder or one [SubtitleSpec.FOLDER_NAMES] folder directly below it, and its name
 * matches the video's name or carries a [SubtitleLanguage] marker. Name matches come first, then languages in
 * [SubtitleLanguage] order; files are taken in that order while the total stays within
 * [SubtitleSpec.MAX_TOTAL_BYTES].
 */
internal object SubtitleFilePlan {
    private val NOT_ALPHANUMERIC = Regex("[^\\p{L}\\p{N}]+")

    fun select(files: List<TorrentFileEntry>, videoIndex: Int, pieceLength: Long): List<SubtitleCandidate> {
        require(pieceLength > 0)
        val video = files.firstOrNull { it.index == videoIndex } ?: return emptyList()
        val videoFolder = folder(video.path)
        val videoName = normalized(SubtitleLanguage.baseName(video.path))
        val ordered = files.asSequence()
            .filter { it.index != videoIndex && SubtitleFormat.of(it.path) != null }
            .filter { it.size in 1..SubtitleSpec.MAX_FILE_BYTES && it.offset >= 0 }
            .filter { inVideoFolder(folder(it.path), videoFolder) }
            .map { candidate(it, videoName, pieceLength) }
            .filter { it.matchesVideoName || it.language != null }
            .sortedWith(compareBy({ !it.matchesVideoName }, { it.language?.ordinal ?: Int.MAX_VALUE }, { it.index }))
        val chosen = mutableListOf<SubtitleCandidate>()
        var totalBytes = 0L
        for (candidate in ordered) {
            if (totalBytes + candidate.size > SubtitleSpec.MAX_TOTAL_BYTES) continue
            totalBytes += candidate.size
            chosen += candidate
        }
        return chosen
    }

    private fun candidate(file: TorrentFileEntry, videoName: String, pieceLength: Long): SubtitleCandidate {
        val name = normalized(SubtitleLanguage.baseName(file.path))
        val matches = videoName.isNotEmpty() && name.isNotEmpty() &&
            (name.startsWith(videoName) || videoName.startsWith(name))
        val first = (file.offset / pieceLength).toInt()
        val last = ((file.offset + file.size - 1) / pieceLength).toInt()
        return SubtitleCandidate(file.index, file.path, file.size, SubtitleLanguage.of(file.path), matches, first..last)
    }

    private fun inVideoFolder(folder: String, videoFolder: String): Boolean {
        if (folder == videoFolder) return true
        val parent = folder.substringBeforeLast('/', "")
        val name = folder.substringAfterLast('/').lowercase(Locale.ROOT)
        return parent == videoFolder && name in SubtitleSpec.FOLDER_NAMES
    }

    private fun folder(path: String): String = path.replace('\\', '/').substringBeforeLast('/', "")

    private fun normalized(name: String): String = name.lowercase(Locale.ROOT).replace(NOT_ALPHANUMERIC, "")
}
