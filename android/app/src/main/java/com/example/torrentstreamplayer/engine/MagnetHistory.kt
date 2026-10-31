package com.example.torrentstreamplayer.engine

import java.io.*
import java.net.URI
import java.net.URLDecoder

data class SavedMagnet(val magnet: String, val title: String, val addedAt: Long,
    val fileSize: Long? = null, val downloadedBytes: Long? = null, val pieces: PieceMapSnapshot? = null,
    val lastPlayedAt: Long = 0)
internal class MagnetHistory(private val cache: TorrentCache) {
    @Synchronized fun list(): List<SavedMagnet> = try {
        val bytes = cache.read("links")
        if (bytes == null) emptyList() else DataInputStream(ByteArrayInputStream(bytes)).use { input ->
            val version = input.readInt()
            val count = if (version == -1 || version == -2) input.readInt() else version
            require(count in 0..50)
            List(count) {
                val magnet = input.readUTF()
                MagnetIdentity.key(magnet)
                val savedTitle = if (version < 0) input.readUTF() else title(magnet)
                val receivedAt = input.readLong()
                SavedMagnet(magnet, savedTitle, receivedAt, lastPlayedAt = if (version == -2) input.readLong() else 0)
            }.sortedByDescending { it.addedAt }
        }
    } catch (_: Exception) { emptyList() }
    @Synchronized fun add(magnet: String, now: Long = System.currentTimeMillis()) {
        require(magnet.length <= 8192)
        val key = MagnetIdentity.key(magnet)
        val previous = list()
        val saved = previous.firstOrNull { MagnetIdentity.key(it.magnet) == key }
        val entries = listOf(SavedMagnet(magnet, saved?.title ?: title(magnet), saved?.addedAt ?: now,
            lastPlayedAt = saved?.lastPlayedAt ?: 0)) +
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
    @Synchronized fun updateTitle(magnet: String, videoTitle: String) {
        val key = MagnetIdentity.key(magnet)
        val cleanTitle = videoTitle.trim().take(512)
        if (cleanTitle.isEmpty()) return
        write(list().map { if (MagnetIdentity.key(it.magnet) == key) it.copy(title = cleanTitle) else it })
    }
    private fun write(entries: List<SavedMagnet>) {
        val buffer = ByteArrayOutputStream()
        DataOutputStream(buffer).use { out ->
            out.writeInt(-2)
            out.writeInt(entries.size)
            entries.forEach { out.writeUTF(it.magnet); out.writeUTF(it.title); out.writeLong(it.addedAt); out.writeLong(it.lastPlayedAt) }
        }
        cache.write("links", buffer.toByteArray())
    }
    private fun title(magnet: String): String {
        val query = URI(magnet).rawSchemeSpecificPart.removePrefix("?")
        return query.split('&').firstOrNull { it.startsWith("dn=") }?.substringAfter('=')
            ?.let { URLDecoder.decode(it, "UTF-8") }?.take(160)
            ?.takeIf { it.isNotBlank() } ?: "토렌트 " + MagnetIdentity.key(magnet).take(8)
    }
}
