package com.example.torrentstreamplayer.engine
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class DownloadStatisticsTest {
    @Test fun onlyVerifiedBytesInsideSelectedFileAreCounted() {
        assertEquals(10L, VerifiedDownload.bytes(10, 3, 4) { true })
        assertEquals(2L, VerifiedDownload.bytes(10, 3, 4) { it == 0 || it == 3 })
        assertEquals(4L, VerifiedDownload.bytes(10, 3, 4) { it == 1 })
        assertEquals(0L, VerifiedDownload.bytes(10, 3, 4) { false })
    }
    @Test fun shortLastPieceDoesNotInflateProgress() {
        assertEquals(1L, VerifiedDownload.bytes(9, 0, 4) { it == 2 })
        assertEquals(9L, VerifiedDownload.bytes(9, 0, 4) { true })
        assertEquals(0L, VerifiedDownload.bytes(0, 0, 4) { true })
    }
    @Test fun preallocatedLengthDoesNotBecomeDownloadedBytesAndDeletedFileResetsProgress() {
        val file = File.createTempFile("download-statistics", ".bin")
        try {
            file.writeBytes(ByteArray(100))
            val snapshot = DownloadSnapshot(100, 30, file.path)
            assertEquals(30L, snapshot.available().downloaded)
            assertEquals(snapshot, DownloadSnapshot.decode(snapshot.encode()))
            assertTrue(file.delete())
            assertEquals(0L, snapshot.available().downloaded)
            assertEquals(100L, snapshot.available().size)
        } finally { file.delete() }
    }
    @Test fun invalidSnapshotCannotClaimMoreThanFileSize() {
        assertThrows(IllegalArgumentException::class.java) { DownloadSnapshot(100, 101, "file") }
        assertThrows(IllegalArgumentException::class.java) { DownloadSnapshot(100, -1, "file") }
        assertThrows(IllegalArgumentException::class.java) { DownloadSnapshot(0, 0, "file") }
    }
    @Test fun oldHistoryWithoutStatisticsRemainsReadable() {
        val entry = SavedMagnet("magnet", "Movie", 1)
        assertNull(entry.fileSize)
        assertNull(entry.downloadedBytes)
    }
}
