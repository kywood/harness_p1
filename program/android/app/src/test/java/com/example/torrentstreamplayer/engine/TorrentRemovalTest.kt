package com.example.torrentstreamplayer.engine
import com.example.torrentstreamplayer.meta.TorrentFailure
import com.example.torrentstreamplayer.meta.UserFacingFailure
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
class TorrentRemovalTest {
    @get:Rule val folder = TemporaryFolder()
    private val key = "a".repeat(64)
    private fun file(root: File, name: String) = File(root, name).apply { parentFile.mkdirs(); writeText("data") }
    @Test fun removesOwnedVideoSidecarsAndAllStateWithoutTouchingOtherTorrents() {
        val downloads = folder.newFolder("downloads")
        val state = folder.newFolder("state")
        file(downloads, "$key/movie/video.mkv"); file(downloads, "$key/movie/subtitle.srt")
        for (kind in listOf("metadata", "resume", "progress", "complete")) file(state, "$key/$kind.bin")
        val keep = file(downloads, "b".repeat(64) + "/other.mkv")
        val keepState = file(state, "b".repeat(64) + "/resume.bin")
        TorrentRemoval(downloads, state).delete(key, emptyList())
        assertFalse(File(downloads, key).exists()); assertFalse(File(state, key).exists())
        assertTrue(keep.exists()); assertTrue(keepState.exists())
        TorrentRemoval(downloads, state).delete(key, emptyList())
    }
    @Test fun legacyRemovalDeletesOnlyMetadataListedFiles() {
        val downloads = folder.newFolder("downloads")
        val state = folder.newFolder("state")
        val video = file(downloads, "old/movie.mkv")
        val subtitle = file(downloads, "old/movie.srt")
        val unrelated = file(downloads, "old/keep.txt")
        TorrentRemoval(downloads, state).delete(key, listOf("old/movie.mkv", "old/movie.srt"))
        assertFalse(video.exists()); assertFalse(subtitle.exists()); assertTrue(unrelated.exists())
    }
    @Test fun traversalFailsBeforeDeletingAnyOwnedFile() {
        val downloads = folder.newFolder("downloads")
        val state = folder.newFolder("state")
        val own = file(downloads, "$key/movie.mkv")
        val outside = folder.newFile("outside.mkv")
        val error = assertThrows(IllegalArgumentException::class.java) {
            TorrentRemoval(downloads, state).delete(key, listOf("../outside.mkv"))
        }
        assertEquals(TorrentFailure.OUTSIDE_STORAGE, (error as UserFacingFailure).failure)
        assertTrue(own.exists()); assertTrue(outside.exists())
    }
    @Test fun anotherTorrentFolderAndStorageRootsCannotBeDeletedAsLegacyFiles() {
        val downloads = folder.newFolder("downloads")
        val state = folder.newFolder("state")
        val other = file(downloads, "b".repeat(64) + "/keep.mkv")
        for (path in listOf("b".repeat(64) + "/keep.mkv", ".", "")) {
            assertThrows(IllegalArgumentException::class.java) { TorrentRemoval(downloads, state).delete(key, listOf(path)) }
        }
        assertTrue(other.exists())
    }
    @Test fun activeFilesPreventDeletionBeforeAnyChanges() {
        val downloads = folder.newFolder("downloads")
        val state = folder.newFolder("state")
        val video = file(downloads, "$key/movie.mkv")
        val metadata = file(state, "$key/metadata.bin")
        val error = assertThrows(IllegalStateException::class.java) {
            TorrentRemoval(downloads, state).delete(key, emptyList(), listOf(video))
        }
        assertEquals(TorrentFailure.ITEM_IN_USE, (error as UserFacingFailure).failure)
        assertTrue(video.exists()); assertTrue(metadata.exists())
    }
    @Test fun invalidIdentityCannotNameStorageRootOrParent() {
        val downloads = folder.newFolder("downloads")
        val state = folder.newFolder("state")
        for (invalid in listOf("", "..", "../other", "A".repeat(64))) {
            assertThrows(IllegalArgumentException::class.java) { TorrentRemoval(downloads, state).delete(invalid, emptyList()) }
        }
        assertTrue(downloads.isDirectory); assertTrue(state.isDirectory)
    }
}
