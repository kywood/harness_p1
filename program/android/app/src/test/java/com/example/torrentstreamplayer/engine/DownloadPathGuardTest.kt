package com.example.torrentstreamplayer.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DownloadPathGuardTest {
    @get:Rule val temp = TemporaryFolder()

    @Test fun pathsInsideTheRootResolveToCanonicalFiles() {
        val root = temp.newFolder("key")
        val expected = File(root, "Movie/Subs/a.srt").canonicalFile
        assertEquals(expected, DownloadPathGuard.resolve(root, "Movie/Subs/../Subs/a.srt"))
        assertTrue(DownloadPathGuard.isInside(root, File(root, "a.srt")))
    }

    @Test fun pathsThatClimbOutAreRejected() {
        val root = temp.newFolder("key")
        assertNull(DownloadPathGuard.resolve(root, "../other/a.srt"))
        assertNull(DownloadPathGuard.resolve(root, "Movie/../../a.srt"))
        assertFalse(DownloadPathGuard.isInside(root, File(root.parentFile, "keyx/a.srt")))
    }
}
