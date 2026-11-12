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
    /** True when its name (or its episode folder's name) matches the video's name word for word. */
    val matchesVideoName: Boolean,
    /**
     * True when the video is the only video in its folder, so a subtitle with another name can still stand in for
     * it. In a season pack (several videos in the folder) only name matches are chosen, and this is false.
     */
    val fallbackAllowed: Boolean,
    val pieces: IntRange,
)

/**
 * Chooses the subtitle files downloaded with the selected video. Pure: no libtorrent, no disk.
 *
 * A file qualifies when its extension is a [SubtitleFormat] and its size is 1..[SubtitleSpec.MAX_FILE_BYTES], and it
 * sits in the video's folder, in a [SubtitleSpec.FOLDER_NAMES] folder below it, or one folder deeper
 * (`Subs/Episode/…`). In a folder with one video it also needs a name match or a [SubtitleLanguage] marker; in a
 * season pack (another video of at least [SubtitleSpec.PACK_VIDEO_MIN_PERCENT] % of its size in the same folder)
 * it needs a name match, so other episodes' subtitles stay out.
 *
 * A name match compares whole words: one name's words must start the other's, so `Show E1` matches `Show E1 ko`
 * and `Show E1 1080p` but not `Show E10`. Name matches come first, then languages in [SubtitleLanguage] order; files
 * are taken in that order while the total stays within [SubtitleSpec.MAX_TOTAL_BYTES].
 */
internal object SubtitleFilePlan {
    fun select(files: List<TorrentFileEntry>, videoIndex: Int, pieceLength: Long): List<SubtitleCandidate> {
        require(pieceLength > 0)
        val video = files.firstOrNull { it.index == videoIndex } ?: return emptyList()
        val videoFolder = folder(video.path)
        val videoWords = SubtitleLanguage.words(SubtitleLanguage.baseName(video.path))
        val packMinBytes = video.size * SubtitleSpec.PACK_VIDEO_MIN_PERCENT / 100
        val singleVideo = files.none { other ->
            other.index != videoIndex && folder(other.path) == videoFolder &&
                TorrentPlayback.isVideo(other.path, other.size) && other.size >= packMinBytes
        }
        val ordered = files.asSequence()
            .filter { it.index != videoIndex && SubtitleFormat.of(it.path) != null }
            .filter { it.size in 1..SubtitleSpec.MAX_FILE_BYTES && it.offset >= 0 }
            .mapNotNull { file -> subtitleFolder(folder(file.path), videoFolder)?.let { file to it } }
            .map { (file, episodeFolder) -> candidate(file, episodeFolder, videoWords, singleVideo, pieceLength) }
            .filter { it.matchesVideoName || (it.fallbackAllowed && it.language != null) }
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

    /** True when the shorter word list starts the longer one. Words never match part of a number (`e1` ≠ `e10`). */
    fun namesMatch(first: List<String>, second: List<String>): Boolean {
        if (first.isEmpty() || second.isEmpty()) return false
        val (shorter, longer) = if (first.size <= second.size) first to second else second to first
        return longer.subList(0, shorter.size) == shorter
    }

    private fun candidate(
        file: TorrentFileEntry,
        episodeFolder: String?,
        videoWords: List<String>,
        singleVideo: Boolean,
        pieceLength: Long,
    ): SubtitleCandidate {
        val nameMatches = namesMatch(SubtitleLanguage.words(SubtitleLanguage.baseName(file.path)), videoWords)
        val folderMatches = episodeFolder != null && namesMatch(SubtitleLanguage.words(episodeFolder), videoWords)
        val first = (file.offset / pieceLength).toInt()
        val last = ((file.offset + file.size - 1) / pieceLength).toInt()
        return SubtitleCandidate(file.index, file.path, file.size, SubtitleLanguage.of(file.path),
            nameMatches || folderMatches, singleVideo, first..last)
    }

    /**
     * Whether [folder] may hold the video's subtitles. Null when it may not; otherwise the episode folder's name
     * (`Subs/<episode>/`) or "" when there is none. The episode folder's name may match the video's name.
     */
    private fun subtitleFolder(folder: String, videoFolder: String): String? {
        if (folder == videoFolder) return ""
        val prefix = if (videoFolder.isEmpty()) "" else "$videoFolder/"
        if (!folder.startsWith(prefix)) return null
        val below = folder.removePrefix(prefix).split('/')
        if (below.size > SubtitleSpec.MAX_FOLDER_DEPTH) return null
        if (below.first().lowercase(Locale.ROOT) !in SubtitleSpec.FOLDER_NAMES) return null
        return below.getOrNull(1).orEmpty()
    }

    private fun folder(path: String): String = path.replace('\\', '/').substringBeforeLast('/', "")
}
