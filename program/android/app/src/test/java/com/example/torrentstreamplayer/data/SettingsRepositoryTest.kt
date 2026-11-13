package com.example.torrentstreamplayer.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

class SettingsRepositoryTest {
    private class MemoryStore(var bytes: ByteArray? = null) : SettingsStore {
        var writes = 0
        override fun read(): ByteArray? = bytes
        override fun write(bytes: ByteArray) {
            this.bytes = bytes
            writes++
        }
    }

    @Test fun defaultsAreWifiOnlyOffAndSubtitlesOn() {
        assertEquals(AppSettings(wifiOnly = false, autoSubtitles = true), AppSettingsCodec.decode(null))
    }

    @Test fun codecRoundTripsAndFallsBackOnDamagedOrUnknownData() {
        val custom = AppSettings(wifiOnly = true, autoSubtitles = false)
        assertEquals(custom, AppSettingsCodec.decode(AppSettingsCodec.encode(custom)))
        assertEquals(AppSettings(), AppSettingsCodec.decode(byteArrayOf(0, 0)))
        val future = AppSettingsCodec.encode(custom).also { it[3] = (AppSettingsCodec.VERSION + 1).toByte() }
        assertEquals(AppSettings(), AppSettingsCodec.decode(future))
    }

    @Test fun subtitleLanguageRoundTripsAndVersionOneStillReads() {
        val custom = AppSettings(autoSubtitles = false, subtitleLanguage = "en")
        assertEquals(custom, AppSettingsCodec.decode(AppSettingsCodec.encode(custom)))
        assertEquals(AppSettings(), AppSettingsCodec.decode(AppSettingsCodec.encode(AppSettings(subtitleLanguage = ""))))
        val versionOne = ByteArrayOutputStream().also { bytes ->
            DataOutputStream(bytes).use { out ->
                out.writeInt(1)
                out.writeBoolean(true)
                out.writeBoolean(false)
            }
        }.toByteArray()
        assertEquals(AppSettings(wifiOnly = true, autoSubtitles = false), AppSettingsCodec.decode(versionOne))
    }

    @Test fun updateSavesAndSurvivesANewRepository() = runBlocking {
        val store = MemoryStore()
        val first = SettingsRepository(store, Dispatchers.Unconfined)
        first.update { it.copy(wifiOnly = true) }
        assertEquals(true, first.settings.value.wifiOnly)
        assertArrayEquals(AppSettingsCodec.encode(AppSettings(wifiOnly = true)), store.bytes)

        val second = SettingsRepository(store, Dispatchers.Unconfined)
        assertEquals(AppSettings(), second.settings.value)
        second.load()
        assertEquals(AppSettings(wifiOnly = true), second.settings.value)
    }

    @Test fun updateBeforeLoadKeepsSavedFieldsAndUnchangedValuesAreNotWritten() = runBlocking {
        val store = MemoryStore(AppSettingsCodec.encode(AppSettings(wifiOnly = true)))
        val repository = SettingsRepository(store, Dispatchers.Unconfined)
        repository.update { it.copy(autoSubtitles = false) }
        assertEquals(AppSettings(wifiOnly = true, autoSubtitles = false), repository.settings.value)
        val writes = store.writes
        repository.update { it }
        assertEquals(writes, store.writes)
    }

    @Test fun emptyStoreLoadsDefaults() = runBlocking {
        val store = MemoryStore()
        SettingsRepository(store, Dispatchers.Unconfined).load()
        assertNull(store.bytes)
    }
}
