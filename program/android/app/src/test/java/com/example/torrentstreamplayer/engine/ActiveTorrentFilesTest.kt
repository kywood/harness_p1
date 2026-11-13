package com.example.torrentstreamplayer.engine
import com.example.torrentstreamplayer.meta.TorrentFailure
import com.example.torrentstreamplayer.meta.UserFacingFailure
import java.io.File
import org.junit.Assert.*
import org.junit.Test
class ActiveTorrentFilesTest {
    @Test fun samePathCannotBeWrittenByTwoEnginesOrDeletedWhileLeased() {
        val first = Any(); val second = Any(); val file = File(System.getProperty("java.io.tmpdir"), "lease-" + System.nanoTime())
        try {
            ActiveTorrentFiles.acquire(first, file)
            assertThrows(IllegalStateException::class.java) { ActiveTorrentFiles.acquire(second, file) }
            val inUse = assertThrows(IllegalStateException::class.java) {
                ActiveTorrentFiles.deleting { ActiveTorrentFiles.requireUnused(file) }
            }
            assertEquals(TorrentFailure.FILE_IN_USE, (inUse as UserFacingFailure).failure)
            ActiveTorrentFiles.release(first)
            ActiveTorrentFiles.acquire(second, file)
        } finally { ActiveTorrentFiles.release(first); ActiveTorrentFiles.release(second) }
    }
    @Test fun differentFilesRemainIndependent() {
        val first = Any(); val second = Any()
        try {
            ActiveTorrentFiles.acquire(first, File(System.getProperty("java.io.tmpdir"), "first-" + System.nanoTime()))
            ActiveTorrentFiles.acquire(second, File(System.getProperty("java.io.tmpdir"), "second-" + System.nanoTime()))
        } finally { ActiveTorrentFiles.release(first); ActiveTorrentFiles.release(second) }
    }
}
