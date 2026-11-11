package com.example.torrentstreamplayer.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class StoredFileKindTest {
    @Test fun registeredVideoExtensionsShowTheVideoIcon() {
        assertEquals(StoredFileKind.VIDEO, StoredFileKind.of("movie.MKV"))
        assertEquals(StoredFileKind.VIDEO, StoredFileKind.of("clip.mp4"))
    }

    @Test fun stateAndUnknownFilesShowTheFileIcon() {
        assertEquals(StoredFileKind.OTHER, StoredFileKind.of("progress.bin"))
        assertEquals(StoredFileKind.OTHER, StoredFileKind.of("README"))
    }
}
