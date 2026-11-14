package com.example.torrentstreamplayer.engine

import com.example.torrentstreamplayer.meta.NetworkSpec
import java.io.*

data class SavedMagnet(val magnet: String, val title: String, val addedAt: Long,
    val fileSize: Long? = null, val downloadedBytes: Long? = null, val pieces: PieceMapSnapshot? = null,
    val lastPlayedAt: Long = 0, val lastPositionMs: Long = 0, val downloadOrder: DownloadOrderPreference? = null)
internal class MagnetHistory(private val cache: TorrentCache) {
    @Synchronized fun list(): List<SavedMagnet> = try {
        val bytes = cache.read("links")
        if (bytes == null) emptyList() else DataInputStream(ByteArrayInputStream(bytes)).use { input ->
            val version = input.readInt()
            val count = if (version in -4..-1) input.readInt() else version
            require(count in 0..50)
            List(count) {
                val magnet = input.readUTF()
                val savedTitle = if (version < 0) input.readUTF() else runCatching { title(magnet) }.getOrDefault("")
                val receivedAt = input.readLong()
                SavedMagnet(magnet, savedTitle, receivedAt, lastPlayedAt = if (version <= -2) input.readLong() else 0,
                    lastPositionMs = if (version <= -3) input.readLong().coerceAtLeast(0) else 0,
                    downloadOrder = if (version <= -4) DownloadOrderPreference.fromCode(input.readByte().toInt()) else null)
            }.filter { runCatching { MagnetIdentity.key(it.magnet) }.isSuccess } // A broken entry is skipped alone.
                .sortedByDescending { it.addedAt }
        }
    } catch (_: Exception) { emptyList() }
    @Synchronized fun add(magnet: String, now: Long = System.currentTimeMillis()) {
        require(magnet.length <= NetworkSpec.MAX_LINK_CHARS)
        val key = MagnetIdentity.key(magnet)
        val previous = list()
        val saved = previous.firstOrNull { MagnetIdentity.key(it.magnet) == key }
        val entries = listOf(SavedMagnet(magnet, saved?.title ?: title(magnet), saved?.addedAt ?: now,
            lastPlayedAt = saved?.lastPlayedAt ?: 0, lastPositionMs = saved?.lastPositionMs ?: 0)) +
            previous.filter { MagnetIdentity.key(it.magnet) != key }
        write(entries.sortedByDescending { it.addedAt }.take(50))
    }
    @Synchronized fun markPlayed(magnet: String, now: Long = System.currentTimeMillis()) {
        require(now >= 0)
        val key = MagnetIdentity.key(magnet)
        write(list().map { if (MagnetIdentity.key(it.magnet) == key) it.copy(lastPlayedAt = maxOf(it.lastPlayedAt, now)) else it })
    }
    @Synchronized fun delete(magnet: String) {
        val key = MagnetIdentity.key(magnet)
        write(list().filter { MagnetIdentity.key(it.magnet) != key })
    }
    @Synchronized fun savePosition(magnet: String, positionMs: Long, durationMs: Long) {
        require(positionMs >= 0 && durationMs >= 0)
        val key = MagnetIdentity.key(magnet)
        val position = PlaybackResume.position(positionMs, durationMs)
        write(list().map { if (MagnetIdentity.key(it.magnet) == key) it.copy(lastPositionMs = position) else it })
    }
    @Synchronized fun updateTitle(magnet: String, videoTitle: String) {
        val key = MagnetIdentity.key(magnet)
        val cleanTitle = videoTitle.trim().take(512)
        if (cleanTitle.isEmpty()) return
        write(list().map { if (MagnetIdentity.key(it.magnet) == key) it.copy(title = cleanTitle) else it })
    }
    private fun write(entries: List<SavedMagnet>) {
        val buffer = ByteArrayOutputStream()
        DataOutputStream(buffer).use { out ->
            out.writeInt(-3)
            out.writeInt(entries.size)
            entries.forEach { out.writeUTF(it.magnet); out.writeUTF(it.title); out.writeLong(it.addedAt); out.writeLong(it.lastPlayedAt); out.writeLong(it.lastPositionMs) }
        }
        cache.write("links", buffer.toByteArray())
    }
    private fun title(magnet: String): String = MagnetDisplayName.of(magnet) ?: PLACEHOLDER + MagnetIdentity.key(magnet).take(8)
    companion object {
        /** 이름 없는 마그넷의 저장된 자리 표시 제목 앞부분. 이미 저장된 값과 맞춰야 해 번역하지 않는다. */
        private const val PLACEHOLDER = "토렌트 "
        /** [title]이 [magnet]의 자리 표시 제목이면 그 짧은 식별자(키 앞 8자리), 아니면 null. 화면이 문구를 바꿔 보인다. */
        fun placeholderId(magnet: String, title: String): String? =
            runCatching { MagnetIdentity.key(magnet).take(8) }.getOrNull()?.takeIf { title == PLACEHOLDER + it }
    }
}
