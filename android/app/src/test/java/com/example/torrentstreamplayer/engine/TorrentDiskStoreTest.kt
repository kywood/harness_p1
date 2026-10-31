package com.example.torrentstreamplayer.engine

import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class TorrentDiskStoreTest {
    @get:Rule val folder = TemporaryFolder()
    private fun store(root: File) = TorrentDiskStore(listOf(root))
    @Test fun listsOnlyManagedFilesWithExactPathsAndSizes() {
        val root = folder.newFolder("downloads")
        val nested = File(root, "torrent").apply { mkdir() }
        val video = File(nested, "video.mp4").apply { writeBytes(ByteArray(42)) }
        folder.newFile("unrelated.txt")
        val listed = store(root).list()
        assertEquals(1, listed.size)
        assertEquals(video.canonicalPath, listed.single().path)
        assertEquals(42L, listed.single().size)
    }
    @Test fun deletesOnlySelectedFileAndPreservesOtherDownloads() {
        val root = folder.newFolder("downloads")
        val first = File(root, "first.mp4").apply { writeBytes(byteArrayOf(1)) }
        val second = File(root, "second.mp4").apply { writeBytes(byteArrayOf(2)) }
        assertTrue(store(root).delete(first.path))
        assertFalse(first.exists())
        assertTrue(second.exists())
        assertFalse(store(root).delete(first.path))
    }
    @Test fun traversalAndSimilarPrefixCannotDeleteOutsideStorage() {
        val root = folder.newFolder("downloads")
        val sibling = folder.newFolder("downloads-other")
        val outside = File(sibling, "keep.txt").apply { writeText("keep") }
        assertThrows(IllegalArgumentException::class.java) { store(root).delete(outside.path) }
        assertThrows(IllegalArgumentException::class.java) {
            store(root).delete(File(root, "../downloads-other/keep.txt").path)
        }
        assertTrue(outside.exists())
    }
    @Test fun activeFileAndDirectoriesCannotBeDeleted() {
        val root = folder.newFolder("downloads")
        val active = File(root, "playing.mp4").apply { writeBytes(byteArrayOf(1)) }
        assertThrows(IllegalArgumentException::class.java) { store(root).delete(active.path, active) }
        val nested = File(root, "nested").apply { mkdir() }
        assertFalse(store(root).delete(nested.path))
        assertTrue(active.exists())
        assertTrue(nested.exists())
    }
    @Test fun emptyAndEvictedCacheListsAreSafe() {
        assertTrue(store(File(folder.root, "missing")).list().isEmpty())
    }
}
