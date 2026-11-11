package com.example.torrentstreamplayer
import com.example.torrentstreamplayer.engine.StoredTorrentFile
import com.example.torrentstreamplayer.ui.StoredFileFilter
import com.example.torrentstreamplayer.ui.BulkSelection
import org.junit.Assert.*
import org.junit.Test

class StoredFileFilterTest {
    private val files = listOf("Movie.MP4", "other.mkv", "resume.bin", "metadata.bin", "README")
        .map { StoredTorrentFile("/cache/$it", it, 10) }
    @Test fun extensionsAreCaseInsensitiveAndUseLastDot() {
        assertEquals("mp4", StoredFileFilter.extension("Movie.MP4"))
        assertEquals("gz", StoredFileFilter.extension("archive.tar.gz"))
        assertEquals("", StoredFileFilter.extension(".hidden"))
        assertEquals("", StoredFileFilter.extension("README"))
        assertEquals("", StoredFileFilter.extension("trailing."))
    }
    @Test fun allAndIndividualExtensionsFilterCorrectly() {
        assertEquals(files, StoredFileFilter.visible(files, null))
        assertEquals(listOf(files[0]), StoredFileFilter.visible(files, "mp4"))
        assertEquals(listOf(files[2], files[3]), StoredFileFilter.visible(files, "bin"))
        assertEquals(listOf(files[4]), StoredFileFilter.visible(files, ""))
        assertTrue(StoredFileFilter.visible(files, "avi").isEmpty())
    }
    @Test fun selectAllAndDeleteAreLimitedToVisibleExtension() {
        val visible = StoredFileFilter.visible(files, "bin")
        val selected = BulkSelection.all(visible.map { it.path }, emptySet())
        val deleted = mutableListOf<String>()
        BulkSelection.delete(selected) { deleted.add(it); true }
        assertEquals(setOf("/cache/resume.bin", "/cache/metadata.bin"), deleted.toSet())
        assertFalse(deleted.contains("/cache/Movie.MP4"))
    }
}
