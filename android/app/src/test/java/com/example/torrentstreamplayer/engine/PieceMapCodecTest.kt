package com.example.torrentstreamplayer.engine
import org.junit.Assert.*
import org.junit.Test
import java.io.*
class PieceMapCodecTest {
    @Test fun persistsAllLevelsAndLegacyPartial() {
        val map = PieceMapSnapshot(PieceCell.entries, 5, 12)
        val saved = DownloadSnapshot(120, 50, "video.mp4", map)
        assertEquals(saved, DownloadSnapshot.decode(saved.encode()))
    }
    @Test fun oldProgressWithoutMapRemainsReadable() {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use {
            it.writeInt(1); it.writeLong(120); it.writeLong(50); it.writeUTF("video.mp4")
        }
        assertEquals(DownloadSnapshot(120, 50, "video.mp4"), DownloadSnapshot.decode(bytes.toByteArray()))
    }
    @Test fun metadataConnectionUpdatesPreserveDownloadedSize() {
        val details = MetadataDetails(downloadedBytes = 1024, videoSize = 4096).connected(3)
        assertEquals(1024, details.downloadedBytes)
        assertEquals(4096, details.videoSize)
        assertEquals(3, details.peers)
    }
}
