package com.example.torrentstreamplayer.ui

import com.example.torrentstreamplayer.engine.SubtitleFile
import com.example.torrentstreamplayer.engine.SubtitleLanguage
import com.example.torrentstreamplayer.meta.PlayerSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SubtitleSessionTest {
    /**
     * Fake player. An added file gets a track only when [listAdded] runs (VLC opens it later); names starting with
     * "refused" are refused at once, names starting with "unreadable" are accepted but never get a track.
     */
    private class FakePlayer(var tracks: PlayerTracks) : TrackPlayer {
        val addedFiles = mutableListOf<File>()
        private val unlisted = ArrayDeque<File>()
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
            if (file.name.startsWith("refused")) return false
            if (!file.name.startsWith("unreadable")) unlisted.addLast(file)
            return true
        }
        fun listAdded() {
            val file = unlisted.removeFirstOrNull() ?: return
            tracks = tracks.copy(subtitles = tracks.subtitles + PlayerTrack(nextId++, "Track ${file.name}"))
        }
    }

    private var clock = 0L
    private val english = PlayerTrack(1, "Track 1 - [English]")
    private val korean = PlayerTrack(2, "Track 2 - [Korean]")
    private val audio = listOf(PlayerTrack(10, "Track 1 - [Japanese]"), PlayerTrack(11, "Track 2 - [Korean]"))
    private fun sub(name: String, language: SubtitleLanguage?) = SubtitleFile(File(name), language, true)
    private fun session(player: FakePlayer, auto: Boolean = true, preferred: String? = null) =
        SubtitleSession(player, auto, preferred) { clock }

    @Test fun beforeStartTheSheetIsLoadingAndShowsDownloadingFiles() {
        val session = session(FakePlayer(PlayerTracks()))
        val sheet = session.updateFiles(emptyList(), listOf(File("Movie.ko.srt"))).sheet
        assertTrue(sheet.loading)
        val started = session.start().sheet
        assertFalse(started.loading)
        assertEquals(OptionTag.DOWNLOADING, started.subtitles.single().tag)
        assertFalse(started.subtitles.single().enabled)
    }

    @Test fun externalFileGetsTheNextNewTrackAndIsPickedOnceWithANotice() {
        val player = FakePlayer(PlayerTracks(listOf(english), audio, audioId = 10))
        val session = session(player)
        assertNull("English is not the default language", session.start().autoPicked)
        val waiting = session.updateFiles(listOf(sub("Movie.ko.srt", SubtitleLanguage.KOREAN)), emptyList())
        assertEquals(listOf(File("Movie.ko.srt")), player.addedFiles)
        assertEquals(PlayerSpec.SUBTITLE_TRACK_TIMEOUT_MS, waiting.recheckAfterMs)
        player.listAdded()
        val snapshot = session.refresh()
        assertEquals(TrackName.File("Movie.ko.srt", TrackLanguage.KOREAN), snapshot.autoPicked)
        assertEquals(100, player.tracks.subtitleId)
        assertEquals(OptionTag.FILE, snapshot.sheet.subtitles.last().tag)
        assertNull(snapshot.recheckAfterMs)
        assertEquals(2, snapshot.sheet.audio.size)
        assertNull("only once", session.refresh().autoPicked)
        assertEquals("ko", session.languageKeyOf(100))
    }

    @Test fun filesAreAddedOneAtATimeAndAnUnreadableOneFailsAfterTheTimeout() {
        val player = FakePlayer(PlayerTracks(listOf(english), audio))
        val session = session(player, auto = false)
        session.start()
        session.updateFiles(listOf(sub("unreadable.ko.smi", SubtitleLanguage.KOREAN),
            sub("Movie.en.srt", SubtitleLanguage.ENGLISH)), emptyList())
        assertEquals("the second file waits for the first one's track", 1, player.addedFiles.size)
        clock += PlayerSpec.SUBTITLE_TRACK_TIMEOUT_MS
        val afterTimeout = session.refresh().sheet
        assertEquals(OptionTag.FAILED, afterTimeout.subtitles.first { it.name == TrackName.File("unreadable.ko.smi",
            TrackLanguage.KOREAN) }.tag)
        assertEquals(2, player.addedFiles.size)
        player.listAdded()
        val sheet = session.refresh().sheet
        val english = sheet.subtitles.single { it.name == TrackName.File("Movie.en.srt", TrackLanguage.ENGLISH) }
        assertEquals("the English file keeps its own track", 100, english.id)
        assertTrue(english.enabled)
    }

    @Test fun aStandInPickGivesWayToThePreferredFileWhenItArrives() {
        val player = FakePlayer(PlayerTracks(listOf(english), audio))
        val session = session(player)
        session.start()
        session.updateFiles(listOf(sub("Movie.en.srt", SubtitleLanguage.ENGLISH)), listOf(File("Movie.ko.srt")))
        player.listAdded()
        assertEquals(TrackName.File("Movie.en.srt", TrackLanguage.ENGLISH), session.refresh().autoPicked)
        assertEquals(100, player.tracks.subtitleId)
        session.updateFiles(listOf(sub("Movie.en.srt", SubtitleLanguage.ENGLISH), sub("Movie.ko.srt",
            SubtitleLanguage.KOREAN)), emptyList())
        player.listAdded()
        assertEquals(TrackName.File("Movie.ko.srt", TrackLanguage.KOREAN), session.refresh().autoPicked)
        assertEquals(101, player.tracks.subtitleId)
    }

    @Test fun aViewerPickBeforeThePreferredFileArrivesIsKept() {
        val player = FakePlayer(PlayerTracks(listOf(english), audio))
        val session = session(player)
        session.start()
        session.updateFiles(emptyList(), listOf(File("Movie.ko.srt")))
        session.selectSubtitle(1)
        session.updateFiles(listOf(sub("Movie.ko.srt", SubtitleLanguage.KOREAN)), emptyList())
        player.listAdded()
        assertNull(session.refresh().autoPicked)
        assertEquals(1, player.tracks.subtitleId)
    }

    @Test fun silentVideosStillGetTheirFiles() {
        val player = FakePlayer(PlayerTracks(videoCount = 1))
        val session = session(player)
        session.start()
        session.updateFiles(listOf(sub("Movie.ko.srt", SubtitleLanguage.KOREAN)), emptyList())
        assertEquals(1, player.addedFiles.size)
    }

    @Test fun preferredLanguageWinsAndEmbeddedTracksCount() {
        val player = FakePlayer(PlayerTracks(listOf(english, korean)))
        val picked = session(player, preferred = "en").start()
        assertEquals(TrackName.Language(TrackLanguage.ENGLISH), picked.autoPicked)
        assertEquals(1, player.tracks.subtitleId)
    }

    @Test fun autoOffKeepsSubtitlesOffUntilTheViewerPicks() {
        val player = FakePlayer(PlayerTracks(listOf(korean), subtitleId = 2))
        val session = session(player, auto = false)
        assertNull(session.start().autoPicked)
        assertEquals(PlayerTracks.NO_TRACK, player.tracks.subtitleId)
        assertTrue(session.selectSubtitle(2))
        assertEquals(2, session.refresh().sheet.selectedSubtitle)
    }

    @Test fun aViewerPickStopsTheAutomaticPick() {
        val player = FakePlayer(PlayerTracks(listOf(english), audio))
        val session = session(player)
        session.start()
        session.selectSubtitle(PlayerTracks.NO_TRACK)
        player.tracks = player.tracks.copy(subtitles = listOf(english, korean))
        assertNull(session.refresh().autoPicked)
        assertEquals(PlayerTracks.NO_TRACK, player.tracks.subtitleId)
    }

    @Test fun refusedPicksAndFilesAreReported() {
        val player = FakePlayer(PlayerTracks(listOf(english), audio))
        val session = session(player, auto = false)
        session.start()
        val sheet = session.updateFiles(listOf(sub("refused.ko.srt", SubtitleLanguage.KOREAN)), emptyList()).sheet
        assertEquals(OptionTag.FAILED, sheet.subtitles.last().tag)
        assertFalse(sheet.subtitles.last().enabled)
        player.refuse = true
        assertFalse(session.selectSubtitle(1))
    }

    @Test fun filesWaitUntilThePlayerListsTheVideosOwnTracks() {
        val player = FakePlayer(PlayerTracks())
        val session = session(player)
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
