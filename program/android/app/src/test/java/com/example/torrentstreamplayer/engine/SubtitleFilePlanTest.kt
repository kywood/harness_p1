package com.example.torrentstreamplayer.engine

import com.example.torrentstreamplayer.meta.SubtitleFormat
import com.example.torrentstreamplayer.meta.SubtitleSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SubtitleFilePlanTest {
    private val pieceLength = 1024L * 1024

    /** Files laid out back to back like libtorrent's file storage. */
    private fun files(vararg entries: Pair<String, Long>): List<TorrentFileEntry> {
        var offset = 0L
        return entries.mapIndexed { index, (path, size) -> TorrentFileEntry(index, path, size, offset).also { offset += size } }
    }

    private fun chosen(files: List<TorrentFileEntry>, videoIndex: Int = 0) =
        SubtitleFilePlan.select(files, videoIndex, pieceLength).map { it.path }

    @Test fun registeredFormatsMatchTheRequest() {
        assertEquals(setOf("srt", "ass", "ssa", "vtt", "sub"), SubtitleFormat.entries.map { it.extension }.toSet())
        assertEquals(SubtitleFormat.SRT, SubtitleFormat.of("A/B.SRT"))
        assertNull(SubtitleFormat.of("movie.smi"))
    }

    @Test fun takesSameNameAndLanguageMarkedSubtitlesNextToTheVideo() {
        val torrent = files("Movie/Movie.2020.mkv" to 700_000_000, "Movie/Movie.2020.srt" to 50_000,
            "Movie/Subs/Korean.srt" to 40_000, "Movie/sample.mkv" to 10_000_000, "Movie/readme.txt" to 100)
        assertEquals(listOf("Movie/Movie.2020.srt", "Movie/Subs/Korean.srt"), chosen(torrent))
    }

    @Test fun skipsUnrelatedFoldersDeepFoldersAndNamesWithoutMarkers() {
        val torrent = files("Show/E01.mkv" to 500_000_000, "Other/E01.srt" to 1_000,
            "Show/Subs/E01/ko.srt" to 1_000, "Show/Extras/E01.srt" to 1_000, "Show/commentary.srt" to 1_000)
        assertEquals(emptyList<String>(), chosen(torrent))
    }

    @Test fun enforcesPerFileAndTotalSizeLimits() {
        val tooBig = files("v.mkv" to 1_000_000, "v.srt" to SubtitleSpec.MAX_FILE_BYTES + 1, "v.ko.ass" to 0)
        assertEquals(emptyList<String>(), chosen(tooBig))
        val many = files("v.mkv" to 1_000_000, *Array(6) { "v.$it.srt" to SubtitleSpec.MAX_FILE_BYTES })
        val plan = SubtitleFilePlan.select(many, 0, pieceLength)
        assertEquals(5, plan.size)
        assertTrue(plan.sumOf { it.size } <= SubtitleSpec.MAX_TOTAL_BYTES)
    }

    @Test fun ordersNameMatchesFirstThenKoreanThenEnglish() {
        val torrent = files("S/Show.E01.mkv" to 100_000_000, "S/Other.eng.srt" to 10, "S/Other.kor.srt" to 10,
            "S/Show.E01.en.srt" to 10, "S/Show.E01.ko.srt" to 10)
        assertEquals(listOf("S/Show.E01.ko.srt", "S/Show.E01.en.srt", "S/Other.kor.srt", "S/Other.eng.srt"),
            chosen(torrent))
    }

    @Test fun piecesCoverTheWholeSubtitleFile() {
        val torrent = files("v.mkv" to pieceLength * 2 - 10, "v.srt" to 20)
        assertEquals(1..2, SubtitleFilePlan.select(torrent, 0, pieceLength).single().pieces)
    }

    @Test fun languageNeedsAWholeTokenForLatinMarkers() {
        assertEquals(SubtitleLanguage.KOREAN, SubtitleLanguage.of("Movie.ko.srt"))
        assertEquals(SubtitleLanguage.KOREAN, SubtitleLanguage.of("Subs/영화 한글자막.srt"))
        assertEquals(SubtitleLanguage.ENGLISH, SubtitleLanguage.of("Subs/2_English.srt"))
        assertNull(SubtitleLanguage.of("King Kong.srt"))
        assertNull(SubtitleLanguage.of("Movie.srt"))
    }
}
