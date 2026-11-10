package com.example.torrentstreamplayer.engine
import java.io.File
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class MagnetHistoryTest {
    @Test fun delayedPositionSaveCannotRestoreDeletedVideo() {
        history().add(magnet(1)); history().delete(magnet(1))
        history().savePosition(magnet(1), 15000, 60000)
        assertTrue(history().list().isEmpty())
    }
    @Test fun versionTwoMigratesWithPlaybackDateAndNoInventedPosition() {
        val bytes = java.io.ByteArrayOutputStream()
        java.io.DataOutputStream(bytes).use { it.writeInt(-2); it.writeInt(1); it.writeUTF(magnet(1)); it.writeUTF("Old"); it.writeLong(12); it.writeLong(34) }
        TorrentCache(folder.root, "0".repeat(64), ::encrypt, ::decrypt).write("links", bytes.toByteArray())
        assertEquals(34L, history().list().single().lastPlayedAt)
        assertEquals(0L, history().list().single().lastPositionMs)
        history().savePosition(magnet(1), 10000, 90000)
        assertEquals(10000L, history().list().single().lastPositionMs)
    }
    @Test fun resettingOneVideoPreservesOtherPositionsAndPlayDates() {
        history().add(magnet(1)); history().add(magnet(2)); history().markPlayed(magnet(1), 100)
        history().savePosition(magnet(1), 42000, 120000); history().savePosition(magnet(2), 90000, 120000)
        history().savePosition(magnet(1), 0, 0)
        assertEquals(0L, history().list().first { it.magnet == magnet(1) }.lastPositionMs)
        assertEquals(100L, history().list().first { it.magnet == magnet(1) }.lastPlayedAt)
        assertEquals(90000L, history().list().first { it.magnet == magnet(2) }.lastPositionMs)
    }
    @Test fun positionSurvivesRestartTrackerChangeAndTitleUpdate() {
        history().add(magnet(1), 10)
        history().savePosition(magnet(1), 42000, 120000)
        history().add(magnet(1) + "&dn=renamed", 20)
        history().updateTitle(magnet(1), "Video")
        assertEquals(42000L, history().list().single().lastPositionMs)
        assertEquals(10L, history().list().single().addedAt)
    }
    @Test fun versionOneHistoryMigratesWithoutInventingPlaybackDate() {
        val buffer = java.io.ByteArrayOutputStream()
        java.io.DataOutputStream(buffer).use { it.writeInt(-1); it.writeInt(1); it.writeUTF(magnet(1)); it.writeUTF("Old"); it.writeLong(12) }
        TorrentCache(folder.root, "0".repeat(64), ::encrypt, ::decrypt).write("links", buffer.toByteArray())
        assertEquals(0L, history().list().single().lastPlayedAt)
        history().markPlayed(magnet(1), 30)
        assertEquals(12L, history().list().single().addedAt)
        assertEquals(30L, history().list().single().lastPlayedAt)
    }
    @Test fun playbackTimestampPersistsIndependentlyFromReceivedDate() {
        history().add(magnet(1), 10)
        history().markPlayed(magnet(1), 100)
        history().add(magnet(1), 200)
        history().markPlayed(magnet(1), 90)
        assertEquals(10L, history().list().single().addedAt)
        assertEquals(100L, history().list().single().lastPlayedAt)
    }
    @get:Rule val folder = TemporaryFolder()
    private val key = SecretKeySpec(ByteArray(16) { it.toByte() }, "AES")
    private fun encrypt(bytes: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        return cipher.iv + cipher.doFinal(bytes)
    }
    private fun decrypt(bytes: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
        return cipher.doFinal(bytes, 12, bytes.size - 12)
    }
    private fun history() = MagnetHistory(TorrentCache(folder.root, "0".repeat(64), ::encrypt, ::decrypt))
    private fun magnet(index: Int) = "magnet:?xt=urn:btih:" + index.toString(16).padStart(40, '0')
    @Test fun savedMagnetSurvivesRestartAndIsNotStoredAsPlaintext() {
        val link = magnet(1) + "&dn=Sample%20Movie"
        history().add(link, 123)
        assertEquals(link, history().list().single().magnet)
        assertEquals("Sample Movie", history().list().single().title)
        assertEquals(123L, history().list().single().addedAt)
        val stored = folder.root.walkTopDown().first { it.isFile }.readBytes()
        assertFalse(String(stored).contains("magnet:"))
    }
    @Test fun sameTorrentIsDeduplicatedWhenTrackersChange() {
        history().add(magnet(1) + "&tr=https%3A%2F%2Fone.test", 1)
        val latest = magnet(1) + "&tr=https%3A%2F%2Ftwo.test"
        history().add(latest, 2)
        assertEquals(1, history().list().size)
        assertEquals(latest, history().list().single().magnet)
    }
    @Test fun latestFiftyAreKeptAndOldestRemoved() {
        for (i in 1..55) history().add(magnet(i), i.toLong())
        val entries = history().list()
        assertEquals(50, entries.size)
        assertEquals(magnet(55), entries.first().magnet)
        assertEquals(magnet(6), entries.last().magnet)
    }
    @Test fun deletingHistoryPreservesVideoAndOtherEntries() {
        val video = folder.newFile("video.mp4").apply { writeBytes(byteArrayOf(7)) }
        history().add(magnet(1))
        history().add(magnet(2))
        history().delete(magnet(1))
        assertEquals(magnet(2), history().list().single().magnet)
        assertTrue(video.exists())
    }
    @Test fun invalidInputIsNotSaved() {
        assertThrows(Exception::class.java) { history().add("not a magnet") }
        assertTrue(history().list().isEmpty())
    }
    @Test fun timestampsDetermineOrderAndRetention() {
        for (i in 1..50) history().add(magnet(i), i.toLong())
        history().add(magnet(51), 0)
        assertEquals(magnet(50), history().list().first().magnet)
        assertEquals(magnet(1), history().list().last().magnet)
        assertFalse(history().list().any { it.magnet == magnet(51) })
    }
    @Test fun videoTitleSurvivesRestartAndReplay() {
        history().add(magnet(1), 10)
        history().updateTitle(magnet(1), "Actual video.mp4")
        assertEquals(10L, history().list().single().addedAt)
        history().add(magnet(1) + "&dn=Other", 20)
        assertEquals("Actual video.mp4", history().list().single().title)
        assertEquals(10L, history().list().single().addedAt)
    }
    @Test fun lateMetadataDoesNotRestoreDeletedEntry() {
        history().add(magnet(1), 10)
        history().delete(magnet(1))
        history().updateTitle(magnet(1), "Deleted.mp4")
        history().markPlayed(magnet(1), 123)
        assertTrue(history().list().isEmpty())
    }
    @Test fun legacyHistoryLoadsAndMigratesWithTitle() {
        val buffer = java.io.ByteArrayOutputStream()
        java.io.DataOutputStream(buffer).use { it.writeInt(1); it.writeUTF(magnet(1)); it.writeLong(123) }
        TorrentCache(folder.root, "0".repeat(64), ::encrypt, ::decrypt).write("links", buffer.toByteArray())
        assertEquals(123L, history().list().single().addedAt)
        history().updateTitle(magnet(1), "Legacy.mp4")
        assertEquals(SavedMagnet(magnet(1), "Legacy.mp4", 123), history().list().single())
    }
}
