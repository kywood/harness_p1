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
    @Test fun unnamedMagnetGetsAPlaceholderTheScreenCanRecognize() {
        history().add(magnet(1))
        val saved = history().list().single()
        val id = MagnetIdentity.key(magnet(1)).take(8)
        assertEquals(id, MagnetHistory.placeholderId(saved.magnet, saved.title))
        assertNull("받은 이름은 자리 표시가 아니다", MagnetHistory.placeholderId(saved.magnet, "Movie.mkv"))
        assertNull("다른 마그넷의 자리 표시와 섞이지 않는다", MagnetHistory.placeholderId(magnet(2), saved.title))
        assertNull(MagnetHistory.placeholderId("not a magnet", saved.title))
    }
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
    @Test fun aBrokenEntrySkipsOnlyItself() {
        val bytes = java.io.ByteArrayOutputStream()
        java.io.DataOutputStream(bytes).use { out ->
            out.writeInt(-3); out.writeInt(2)
            listOf(magnet(1), "magnet:?xt=urn:btih:broken").forEachIndexed { index, link ->
                out.writeUTF(link); out.writeUTF("T$index"); out.writeLong(10L + index); out.writeLong(0); out.writeLong(0)
            }
        }
        TorrentCache(folder.root, "0".repeat(64), ::encrypt, ::decrypt).write("links", bytes.toByteArray())
        assertEquals(listOf(magnet(1)), history().list().map { it.magnet })
        history().add(magnet(2), 20)
        history().savePosition(magnet(1), 42000, 120000)
        assertEquals("writing again keeps the good entries", setOf(magnet(1), magnet(2)),
            history().list().map { it.magnet }.toSet())
        assertEquals(42000L, history().list().first { it.magnet == magnet(1) }.lastPositionMs)
    }
    @Test fun aBrokenEntryInThePositiveVersionFormatSkipsOnlyItself() {
        val bytes = java.io.ByteArrayOutputStream()
        java.io.DataOutputStream(bytes).use { out ->
            out.writeInt(2) // the oldest format: the count, then link and date without a title
            out.writeUTF("magnet:?xt=urn:btih:broken"); out.writeLong(5)
            out.writeUTF(magnet(1)); out.writeLong(6)
        }
        TorrentCache(folder.root, "0".repeat(64), ::encrypt, ::decrypt).write("links", bytes.toByteArray())
        val entry = history().list().single()
        assertEquals(magnet(1), entry.magnet)
        assertEquals(6L, entry.addedAt)
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
    @Test fun versionFourReadsTheDownloadOrderAndDropsUnknownCodes() {
        val bytes = java.io.ByteArrayOutputStream()
        java.io.DataOutputStream(bytes).use { out ->
            out.writeInt(-4); out.writeInt(3)
            listOf(DownloadOrderPreference.FASTEST.code, DownloadOrderPreference.NO_CODE, 99).forEachIndexed { index, code ->
                out.writeUTF(magnet(index + 1)); out.writeUTF("T$index"); out.writeLong(30L - index)
                out.writeLong(5); out.writeLong(7000); out.writeByte(code)
            }
        }
        TorrentCache(folder.root, "0".repeat(64), ::encrypt, ::decrypt).write("links", bytes.toByteArray())
        val entries = history().list()
        assertEquals(listOf(DownloadOrderPreference.FASTEST, null, null), entries.map { it.downloadOrder })
        assertEquals(listOf(7000L, 7000L, 7000L), entries.map { it.lastPositionMs })
        assertEquals(listOf(magnet(1), magnet(2), magnet(3)), entries.map { it.magnet })
    }
    @Test fun versionThreeEntriesReadUnchangedWithoutADownloadOrder() {
        writeVersionThree()
        assertEquals(listOf(SavedMagnet(magnet(1), "First", 20, lastPlayedAt = 30, lastPositionMs = 42000),
            SavedMagnet(magnet(2), "Second", 10)), history().list())
    }
    @Test fun writingMigratesVersionThreeToFourAndKeepsEveryEntry() {
        writeVersionThree()
        history().markPlayed(magnet(2), 99)
        assertEquals(-4, savedVersion())
        assertEquals(listOf(SavedMagnet(magnet(1), "First", 20, lastPlayedAt = 30, lastPositionMs = 42000),
            SavedMagnet(magnet(2), "Second", 10, lastPlayedAt = 99)), history().list())
    }
    @Test fun downloadOrderSurvivesEveryOtherWrite() {
        val bytes = java.io.ByteArrayOutputStream()
        java.io.DataOutputStream(bytes).use { out ->
            out.writeInt(-4); out.writeInt(1); out.writeUTF(magnet(1)); out.writeUTF("T"); out.writeLong(10)
            out.writeLong(0); out.writeLong(0); out.writeByte(DownloadOrderPreference.FASTEST.code)
        }
        TorrentCache(folder.root, "0".repeat(64), ::encrypt, ::decrypt).write("links", bytes.toByteArray())
        history().add(magnet(1) + "&dn=again", 50)
        history().add(magnet(2), 60)
        history().markPlayed(magnet(1), 70)
        history().savePosition(magnet(1), 15000, 90000)
        history().updateTitle(magnet(1), "Video.mkv")
        assertEquals(DownloadOrderPreference.FASTEST, history().list().first { it.title == "Video.mkv" }.downloadOrder)
        assertNull(history().list().first { it.magnet == magnet(2) }.downloadOrder)
    }
    @Test fun downloadOrderIsSavedPerVideoAndClearedWithNull() {
        history().add(magnet(1), 10); history().add(magnet(2), 20)
        history().savePosition(magnet(1), 15000, 90000)
        history().setDownloadOrder(magnet(1) + "&tr=https%3A%2F%2Fother.test", DownloadOrderPreference.FASTEST)
        assertEquals(DownloadOrderPreference.FASTEST, history().list().first { it.magnet == magnet(1) }.downloadOrder)
        assertEquals(15000L, history().list().first { it.magnet == magnet(1) }.lastPositionMs)
        assertNull(history().list().first { it.magnet == magnet(2) }.downloadOrder)
        history().setDownloadOrder(magnet(1), DownloadOrderPreference.FROM_START)
        assertEquals(DownloadOrderPreference.FROM_START, history().list().first { it.magnet == magnet(1) }.downloadOrder)
        history().setDownloadOrder(magnet(1), null)
        assertNull(history().list().first { it.magnet == magnet(1) }.downloadOrder)
    }
    @Test fun settingTheOrderOfADeletedVideoDoesNotBringItBack() {
        history().add(magnet(1)); history().delete(magnet(1))
        history().setDownloadOrder(magnet(1), DownloadOrderPreference.FASTEST)
        assertTrue(history().list().isEmpty())
    }
    private fun savedVersion(): Int = java.io.DataInputStream(java.io.ByteArrayInputStream(
        TorrentCache(folder.root, "0".repeat(64), ::encrypt, ::decrypt).read("links"))).use { it.readInt() }
    /** Two entries in the -3 format (no download order), as the released app wrote them. */
    private fun writeVersionThree() {
        val bytes = java.io.ByteArrayOutputStream()
        java.io.DataOutputStream(bytes).use { out ->
            out.writeInt(-3); out.writeInt(2)
            out.writeUTF(magnet(1)); out.writeUTF("First"); out.writeLong(20); out.writeLong(30); out.writeLong(42000)
            out.writeUTF(magnet(2)); out.writeUTF("Second"); out.writeLong(10); out.writeLong(0); out.writeLong(0)
        }
        TorrentCache(folder.root, "0".repeat(64), ::encrypt, ::decrypt).write("links", bytes.toByteArray())
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
