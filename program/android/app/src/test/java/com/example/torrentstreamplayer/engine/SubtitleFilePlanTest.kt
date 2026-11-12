package com.example.torrentstreamplayer.engine

import com.example.torrentstreamplayer.meta.SubtitleFormat
import com.example.torrentstreamplayer.meta.SubtitleSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SubtitleFilePlanTest {
    private val pieceLength = 1024L * 1024

    /** Files laid out back to back like libtorrent's file storage. */
    private fun files(vararg entries: Pair<String, Long>): List<TorrentFileEntry> {
        var offset = 0L
        return entries.mapIndexed { index, (path, size) ->
            TorrentFileEntry(index, path, size, offset).also { offset += size }
        }
    }

    private fun chosen(files: List<TorrentFileEntry>, videoIndex: Int = 0) =
        SubtitleFilePlan.select(files, videoIndex, pieceLength).map { it.path }

    @Test fun registeredFormatsMatchTheRequest() {
        val extensions = SubtitleFormat.entries.map { it.extension }.toSet()
        assertEquals(setOf("srt", "ass", "ssa", "vtt", "sub", "smi"), extensions)
        assertEquals(SubtitleFormat.SRT, SubtitleFormat.of("A/B.SRT"))
        assertEquals(SubtitleFormat.SMI, SubtitleFormat.of("영화.SMI"))
        assertNull(SubtitleFormat.of("movie.txt"))
    }

    @Test fun takesSameNameAndLanguageMarkedSubtitlesNextToTheVideo() {
        val torrent = files("Movie/Movie.2020.mkv" to 700_000_000, "Movie/Movie.2020.srt" to 50_000,
            "Movie/Subs/Korean.srt" to 40_000, "Movie/sample.mkv" to 10_000_000, "Movie/readme.txt" to 100)
        assertEquals(listOf("Movie/Movie.2020.srt", "Movie/Subs/Korean.srt"), chosen(torrent))
    }

    @Test fun skipsUnrelatedFoldersTooDeepFoldersAndNamesWithoutMarkers() {
        val torrent = files("Show/E01.mkv" to 500_000_000, "Other/E01.srt" to 1_000,
            "Show/Subs/E01/Extra/E01.srt" to 1_000, "Show/Extras/E01.srt" to 1_000, "Show/commentary.srt" to 1_000, "Show/trivia.srt" to 1_000)
        assertEquals(emptyList<String>(), chosen(torrent))
    }

    @Test fun aLoneVideosOnlySubtitleIsTakenWithoutALanguageMarker() {
        val lone = files("M/Movie.mkv" to 500_000_000, "M/Subs/subtitle.srt" to 1_000)
        assertEquals(listOf("M/Subs/subtitle.srt"), chosen(lone))
        val pack = files("S/Show E1.mkv" to 500_000_000, "S/Show E2.mkv" to 500_000_000, "S/subtitle.srt" to 1_000)
        assertEquals("not in a season pack", emptyList<String>(), chosen(pack))
    }

    @Test fun searchesTwoFoldersDownAndMatchesTheEpisodeFolderName() {
        val torrent = files("Show/Show.S01E01.mkv" to 500_000_000, "Show/Subs/Show.S01E01/2_English.srt" to 1_000,
            "Show/Subs/Show.S01E01/3_Korean.smi" to 1_000, "Show/Subs/Other/Notes.srt" to 1_000)
        assertEquals(listOf("Show/Subs/Show.S01E01/3_Korean.smi", "Show/Subs/Show.S01E01/2_English.srt"),
            chosen(torrent))
    }

    @Test fun nameMatchesNeedWholeWordsSoE1IsNotE10() {
        assertTrue(SubtitleFilePlan.namesMatch(listOf("show", "e1"), listOf("show", "e1", "ko")))
        assertTrue(SubtitleFilePlan.namesMatch(listOf("show", "e1", "1080p"), listOf("show", "e1")))
        assertFalse(SubtitleFilePlan.namesMatch(listOf("show", "e1"), listOf("show", "e10")))
        assertFalse(SubtitleFilePlan.namesMatch(emptyList(), listOf("show")))
    }

    @Test fun seasonPackTakesOnlyThisEpisodesSubtitles() {
        val torrent = files("S/Show E1.mkv" to 500_000_000, "S/Show E2.mkv" to 500_000_000,
            "S/Show E10.srt" to 1_000, "S/Show E2.ko.srt" to 1_000, "S/Subs/Korean.srt" to 1_000,
            "S/Show E1.ko.srt" to 1_000)
        val plan = SubtitleFilePlan.select(torrent, 0, pieceLength)
        assertEquals(listOf("S/Show E1.ko.srt"), plan.map { it.path })
        assertFalse(plan.single().fallbackAllowed)
        val noOwnSubtitle = torrent.filterNot { it.path == "S/Show E1.ko.srt" }
        assertEquals("no other episode stands in", emptyList<String>(), chosen(noOwnSubtitle))
    }

    @Test fun samplesDoNotMakeASeasonPack() {
        val torrent = files("M/Movie.mkv" to 1_000_000_000, "M/Sample.mkv" to 20_000_000, "M/Korean.srt" to 1_000)
        val plan = SubtitleFilePlan.select(torrent, 0, pieceLength)
        assertEquals(listOf("M/Korean.srt"), plan.map { it.path })
        assertTrue(plan.single().fallbackAllowed)
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

    @Test fun aSmallerEpisodeNextToALargeSpecialStillMakesASeasonPack() {
        val torrent = files("S/Show.S01E00.Special.mkv" to 4_000_000_000, "S/Show.S01E01.mkv" to 900_000_000,
            "S/Show.S01E01.ko.srt" to 10)
        assertFalse(SubtitleFilePlan.select(torrent, 0, pieceLength).any { it.fallbackAllowed })
        assertEquals(emptyList<String>(), chosen(torrent))
    }

    @Test fun oneLargeExtraNextToAMovieIsNotASeasonPack() {
        val torrent = files("M/Movie.mkv" to 4_000_000_000, "M/Behind.The.Scenes.mkv" to 1_200_000_000,
            "M/Subs/Korean.srt" to 10)
        val plan = SubtitleFilePlan.select(torrent, 0, pieceLength)
        assertEquals(listOf("M/Subs/Korean.srt"), plan.map { it.path })
        assertTrue(plan.single().fallbackAllowed)
    }

    @Test fun aSampleOfTheSameEpisodeIsNotAnotherEpisode() {
        val torrent = files("S/Show.S01E01.mkv" to 900_000_000, "S/Show.S01E01.sample.mkv" to 30_000_000,
            "S/Korean.srt" to 10)
        assertEquals(listOf("S/Korean.srt"), chosen(torrent))
    }

    @Test fun threeLargeVideosWithoutMarkersAreASeasonPack() {
        val torrent = files("S/Show 01.mkv" to 500_000_000, "S/Show 02.mkv" to 500_000_000,
            "S/Show 03.mkv" to 500_000_000, "S/Korean.srt" to 10)
        assertEquals(emptyList<String>(), chosen(torrent))
    }

    @Test fun aSeasonPackIgnoresNamesWithoutAnEpisodeMarker() {
        val torrent = files("S/Show.S01E01.mkv" to 500_000_000, "S/Show.S01E02.mkv" to 500_000_000,
            "S/Show.srt" to 10, "S/Subs/Show.S01E01/English.srt" to 10)
        assertEquals(listOf("S/Subs/Show.S01E01/English.srt"), chosen(torrent))
        val single = files("M/Show.2020.mkv" to 500_000_000, "M/Show.srt" to 10)
        assertEquals("a lone video still matches a short name", listOf("M/Show.srt"), chosen(single))
    }

    @Test fun releaseWordsAreLeftOutOfTheNameComparison() {
        val torrent = files("S/Show.S01E01.1080p.WEB.H264-GRP.mkv" to 500_000_000, "S/Show.S01E01.ko.srt" to 10,
            "S/Show.S01E02.ko.srt" to 10)
        val plan = SubtitleFilePlan.select(torrent, 0, pieceLength)
        assertEquals(listOf("S/Show.S01E01.ko.srt"), plan.filter { it.matchesVideoName }.map { it.path })
        assertTrue(SubtitleSpec.QUALITY_WORD.matches("2160p"))
        assertFalse(SubtitleSpec.QUALITY_WORD.matches("show"))
    }

    @Test fun languageNeedsAWholeTokenForLatinMarkers() {
        assertEquals(SubtitleLanguage.KOREAN, SubtitleLanguage.of("Movie.ko.srt"))
        assertEquals(SubtitleLanguage.KOREAN, SubtitleLanguage.of("Subs/영화 한글자막.srt"))
        assertEquals(SubtitleLanguage.ENGLISH, SubtitleLanguage.of("Subs/2_English.srt"))
        assertNull(SubtitleLanguage.of("King Kong.srt"))
        assertNull(SubtitleLanguage.of("Movie.srt"))
    }
}
