package com.example.torrentstreamplayer.engine

import com.example.torrentstreamplayer.meta.TorrentStorageDir
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class TorrentStorageTest {
    @get:Rule val temp = TemporaryFolder()

    private fun storage() = TorrentStorage(File(temp.root, "files"), File(temp.root, "cache"))
    private fun legacy(kind: TorrentStorageDir, path: String, text: String = path) =
        File(File(temp.root, "cache/" + kind.dirName), path).apply { parentFile.mkdirs(); writeText(text) }

    @Test fun storageDirectoriesUseRegisteredNamesUnderPersistentRoot() {
        val storage = storage()
        TorrentStorageDir.entries.forEach {
            assertEquals(File(temp.root, "files/" + it.dirName), storage.dir(it))
        }
    }

    @Test fun missingLegacyFoldersAreANoOp() {
        assertEquals(TorrentStorage.MigrationResult(), storage().migrateLegacy())
        assertFalse(File(temp.root, "files").exists())
    }

    @Test fun legacyFoldersMoveWholeWhenTargetIsEmpty() {
        legacy(TorrentStorageDir.DOWNLOADS, "aa/video.mp4", "video")
        legacy(TorrentStorageDir.STATE, "aa/resume.bin", "resume")
        val storage = storage()

        val result = storage.migrateLegacy()

        assertEquals(2, result.moved)
        assertEquals("video", File(storage.dir(TorrentStorageDir.DOWNLOADS), "aa/video.mp4").readText())
        assertEquals("resume", File(storage.dir(TorrentStorageDir.STATE), "aa/resume.bin").readText())
        assertFalse(File(temp.root, "cache/torrent_downloads").exists())
        assertFalse(File(temp.root, "cache/torrent_state").exists())
    }

    @Test fun legacyEntriesMergeIntoExistingTargetFolder() {
        val storage = storage()
        File(storage.dir(TorrentStorageDir.DOWNLOADS), "new/video.mp4").apply { parentFile.mkdirs(); writeText("new") }
        legacy(TorrentStorageDir.DOWNLOADS, "old/video.mp4", "old")

        storage.migrateLegacy()

        assertEquals("new", File(storage.dir(TorrentStorageDir.DOWNLOADS), "new/video.mp4").readText())
        assertEquals("old", File(storage.dir(TorrentStorageDir.DOWNLOADS), "old/video.mp4").readText())
        assertFalse(File(temp.root, "cache/torrent_downloads").exists())
    }

    @Test fun conflictingFileKeepsTargetAndLeavesLegacyCopy() {
        val storage = storage()
        val target = File(storage.dir(TorrentStorageDir.STATE), "aa/progress.bin").apply { parentFile.mkdirs(); writeText("current") }
        val old = legacy(TorrentStorageDir.STATE, "aa/progress.bin", "stale")

        val result = storage.migrateLegacy()

        assertEquals(1, result.kept)
        assertEquals("current", target.readText())
        assertTrue(old.isFile)
    }
}
