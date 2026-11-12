package com.example.torrentstreamplayer.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SubtitleDownloadsTest {
    @get:Rule val temp = TemporaryFolder()
    private val pieceLength = 100L

    private fun entries(vararg files: Pair<String, Long>): List<TorrentFileEntry> {
        var offset = 0L
        return files.mapIndexed { index, (path, size) ->
            TorrentFileEntry(index, path, size, offset).also { offset += size }
        }
    }

    @Test fun publishesOnlyFilesWhosePiecesAreAllVerified() {
        val subtitles = SubtitleDownloads()
        subtitles.select(entries("v.mkv" to 250, "v.ko.srt" to 100, "v.en.srt" to 100), 0, pieceLength, temp.root)
        assertEquals(setOf(1, 2), subtitles.fileIndexes)
        assertTrue(subtitles.hasPending)
        assertEquals(listOf("v.ko.srt", "v.en.srt"), subtitles.pending.value.map { it.name })
        // v.ko.srt spans pieces 2..3, v.en.srt spans 3..4.
        assertFalse(subtitles.track { it == 2 })
        assertTrue(subtitles.track { it in 2..3 })
        assertEquals(listOf(SubtitleLanguage.KOREAN), subtitles.files.value.map { it.language })
        assertEquals(listOf("v.en.srt"), subtitles.pending.value.map { it.name })
        assertTrue(subtitles.hasPending)
        assertTrue(subtitles.track { true })
        assertFalse(subtitles.hasPending)
        assertEquals(emptyList<String>(), subtitles.pending.value)
        assertFalse("nothing new", subtitles.track { true })
    }

    @Test fun dropsPathsThatEscapeTheDownloadFolder() {
        val subtitles = SubtitleDownloads()
        val root = temp.newFolder("key")
        // Same folder as the video by name, so the plan keeps it; only the canonical path check removes it.
        assertEquals(1, SubtitleFilePlan.select(entries("../v.mkv" to 10, "../v.srt" to 10), 0, pieceLength).size)
        subtitles.select(entries("../v.mkv" to 10, "../v.srt" to 10), 0, pieceLength, root)
        assertEquals(emptySet<Int>(), subtitles.fileIndexes)
        subtitles.select(entries("v.mkv" to 10, "v.srt" to 10), 0, pieceLength, root)
        assertEquals(setOf(1), subtitles.fileIndexes)
    }

    @Test fun restoresSavedCompletionOnlyForFilesOnDisk() {
        val root = temp.newFolder("key")
        val plan = entries("v.mkv" to 250, "v.ko.srt" to 4, "v.en.srt" to 4)
        val first = SubtitleDownloads()
        first.select(plan, 0, pieceLength, root)
        first.track { true }
        val saved = first.encodeCompleted()

        root.resolve("v.ko.srt").writeBytes(ByteArray(4))
        val restored = SubtitleDownloads()
        restored.select(plan, 0, pieceLength, root)
        restored.restore(saved)
        assertEquals(listOf("v.ko.srt"), restored.files.value.map { it.file.name })

        restored.restore(byteArrayOf(1, 2))
        restored.clear()
        assertEquals(emptyList<SubtitleFile>(), restored.files.value)
    }
}
