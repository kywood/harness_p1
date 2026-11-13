package com.example.torrentstreamplayer.ui

import com.example.torrentstreamplayer.engine.SubtitleFile
import com.example.torrentstreamplayer.engine.SubtitleLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SubtitleSessionTest {
    /** Fake player: an added file becomes a new subtitle track only when [listAdded] runs (VLC adds it later). */
    private class FakePlayer(var tracks: PlayerTracks) : TrackPlayer {
        val addedFiles = mutableListOf<File>()
        var refuse = false
        private var nextId = 100
        override fun tracks() = tracks
        override fun selectSubtitle(id: Int): Boolean {
            if (refuse) return false
            tracks = tracks.copy(subtitleId = id)
            return true
        }
        override fun selectAudio(id: Int): Boolean {
            tracks = tracks.copy(audioId = id)
            return true
        }
        override fun addSubtitle(file: File): Boolean {
            addedFiles += file
            return !file.name.startsWith("broken")
        }
        fun listAdded() {
            val newTracks = addedFiles.filterNot { it.name.startsWith("broken") }.drop(tracks.subtitles.count { it.id >= 100 })
                .map { PlayerTrack(nextId++, "Track ${nextId - 100}") }
            tracks = tracks.copy(subtitles = tracks.subtitles + newTracks)
        }
    }

    private val english = PlayerTrack(1, "Track 1 - [English]")
    private val korean = PlayerTrack(2, "Track 2 - [Korean]")
    private val audio = listOf(PlayerTrack(10, "Track 1 - [Japanese]"), PlayerTrack(11, "Track 2 - [Korean]"))
    private fun sub(name: String, language: SubtitleLanguage?) = SubtitleFile(File(name), language, true)

    @Test fun beforeStartTheSheetIsLoadingAndShowsDownloadingFiles() {
        val session = SubtitleSession(FakePlayer(PlayerTracks()), autoEnabled = true, preferredKey = null)
        val sheet = session.updateFiles(emptyList(), listOf(File("Movie.ko.srt"))).sheet
        assertTrue(sheet.loading)
        val started = session.start().sheet
        assertFalse(started.loading)
        assertEquals(OptionTag.DOWNLOADING, started.subtitles.single().tag)
        assertFalse(started.subtitles.single().enabled)
    }

    @Test fun externalFileGetsTheNextNewTrackAndIsPickedOnceWithANotice() {
        val player = FakePlayer(PlayerTracks(listOf(english), audio, audioId = 10))
        val session = SubtitleSession(player, autoEnabled = true, preferredKey = null)
        assertNull("English is not the default language", session.start().autoPicked)
        session.updateFiles(listOf(sub("Movie.ko.srt", SubtitleLanguage.KOREAN)), emptyList())
        assertEquals(listOf(File("Movie.ko.srt")), player.addedFiles)
        player.listAdded()
        val snapshot = session.refresh()
        assertEquals(TrackName.File("Movie.ko.srt", TrackLanguage.KOREAN), snapshot.autoPicked)
        assertEquals(100, player.tracks.subtitleId)
        assertEquals(OptionTag.FILE, snapshot.sheet.subtitles.last().tag)
        assertEquals(2, snapshot.sheet.audio.size)
        assertNull("only once", session.refresh().autoPicked)
        assertEquals("ko", session.languageKeyOf(100))
    }

    @Test fun preferredLanguageWinsAndEmbeddedTracksCount() {
        val player = FakePlayer(PlayerTracks(listOf(english, korean)))
        val picked = SubtitleSession(player, autoEnabled = true, preferredKey = "en").start()
        assertEquals(TrackName.Language(TrackLanguage.ENGLISH), picked.autoPicked)
        assertEquals(1, player.tracks.subtitleId)
    }

    @Test fun autoOffKeepsSubtitlesOffUntilTheViewerPicks() {
        val player = FakePlayer(PlayerTracks(listOf(korean), subtitleId = 2))
        val session = SubtitleSession(player, autoEnabled = false, preferredKey = null)
        val start = session.start()
        assertNull(start.autoPicked)
        assertEquals(PlayerTracks.NO_TRACK, player.tracks.subtitleId)
        assertTrue(session.selectSubtitle(2))
        assertEquals(2, session.refresh().sheet.selectedSubtitle)
    }

    @Test fun aViewerPickStopsTheAutomaticPick() {
        val player = FakePlayer(PlayerTracks(listOf(english), audio))
        val session = SubtitleSession(player, autoEnabled = true, preferredKey = null)
        session.start()
        session.selectSubtitle(PlayerTracks.NO_TRACK)
        player.tracks = player.tracks.copy(subtitles = listOf(english, korean))
        assertNull(session.refresh().autoPicked)
        assertEquals(PlayerTracks.NO_TRACK, player.tracks.subtitleId)
    }

    @Test fun refusedPicksAndBrokenFilesAreReported() {
        val player = FakePlayer(PlayerTracks(listOf(english), audio))
        val session = SubtitleSession(player, autoEnabled = false, preferredKey = null)
        session.start()
        val sheet = session.updateFiles(listOf(sub("broken.ko.srt", SubtitleLanguage.KOREAN)), emptyList()).sheet
        assertEquals(OptionTag.FAILED, sheet.subtitles.last().tag)
        assertFalse(sheet.subtitles.last().enabled)
        player.refuse = true
        assertFalse(session.selectSubtitle(1))
    }

    @Test fun filesWaitUntilThePlayerListsTheVideosOwnTracks() {
        val player = FakePlayer(PlayerTracks())
        val session = SubtitleSession(player, autoEnabled = true, preferredKey = null)
        session.updateFiles(listOf(sub("Movie.ko.srt", SubtitleLanguage.KOREAN)), emptyList())
        session.start()
        assertTrue("no tracks yet", player.addedFiles.isEmpty())
        player.tracks = PlayerTracks(listOf(english), audio)
        session.refresh()
        assertEquals(1, player.addedFiles.size)
        assertEquals("the embedded track is not taken for the file", 1, session.refresh().sheet.subtitles.count {
            it.name is TrackName.Language
        })
    }
}
