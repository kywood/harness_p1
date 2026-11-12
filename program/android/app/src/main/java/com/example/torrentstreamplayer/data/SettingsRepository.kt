package com.example.torrentstreamplayer.data

import android.content.Context
import com.example.torrentstreamplayer.engine.TorrentCache
import com.example.torrentstreamplayer.engine.TorrentCacheCipher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Where [SettingsRepository] keeps its bytes. Tests pass an in-memory store. */
internal interface SettingsStore {
    fun read(): ByteArray?
    fun write(bytes: ByteArray)
}

/**
 * 설정 저장소. [settings]는 읽기 전용이고 바꾸려면 [update]를 부른다. 디스크 읽기·쓰기는 [io]에서 한다.
 *
 * 저장 방식: 설정은 개인정보가 아니라 평문이어도 되지만, 디스크에 쓰는 상태는 모두 [TorrentCache] +
 * [TorrentCacheCipher]를 거친다는 program 규칙과 같은 방식으로 저장한다. DataStore는 새 의존성이라 쓰지 않는다.
 */
internal class SettingsRepository(
    private val store: SettingsStore,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    private val mutableSettings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = mutableSettings.asStateFlow()
    private val lock = Mutex()
    private var loaded = false

    /** Reads saved settings once. Until then [settings] holds the defaults. */
    suspend fun load() = lock.withLock { ensureLoaded() }

    /** Applies [change] to the current settings and saves the result. A failed save keeps the new value in memory. */
    suspend fun update(change: (AppSettings) -> AppSettings) = lock.withLock {
        ensureLoaded()
        val next = change(mutableSettings.value)
        if (next == mutableSettings.value) return@withLock
        mutableSettings.value = next
        withContext(io) { store.write(AppSettingsCodec.encode(next)) }
    }

    private suspend fun ensureLoaded() {
        if (loaded) return
        mutableSettings.value = AppSettingsCodec.decode(withContext(io) { store.read() })
        loaded = true
    }

    companion object {
        /** 설정 파일 폴더(noBackupFilesDir 아래)·항목 이름. 바꾸면 저장된 설정을 읽지 못한다. */
        private const val SETTINGS_DIR = "app_settings"
        private const val SETTINGS_KIND = "settings"

        fun create(context: Context): SettingsRepository {
            val cache = AppStateCache.create(context, SETTINGS_DIR)
            return SettingsRepository(object : SettingsStore {
                override fun read(): ByteArray? = cache.read(SETTINGS_KIND)
                override fun write(bytes: ByteArray) = cache.write(SETTINGS_KIND, bytes)
            })
        }
    }
}
