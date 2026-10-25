package com.example.torrentstreamplayer.engine

import java.io.*
import java.net.URI
import java.net.URLDecoder

data class SavedMagnet(val magnet: String, val title: String, val addedAt: Long,
    val fileSize: Long? = null, val downloadedBytes: Long? = null, val pieces: PieceMapSnapshot? = null)
internal class MagnetHistory(private val cache: TorrentCache) {
    @Synchronized fun list(): List<SavedMagnet> = try {
        val bytes = cache.read("links")
        if (bytes == null) emptyList() else DataInputStream(ByteArrayInputStream(bytes)).use { input ->
            val version = input.readInt()
            val count = if (version == -1) input.readInt() else version
            require(count in 0..50)
            List(count) {
                val magnet = input.readUTF()
                MagnetIdentity.key(magnet)
                val savedTitle = if (version == -1) input.readUTF() else title(magnet)
                SavedMagnet(magnet, savedTitle, input.readLong())
            }.sortedByDescending { it.addedAt }
        }
    } catch (_: Exception) { emptyList() }
    @Synchronized fun add(magnet: String, now: Long = System.currentTimeMillis()) {
        require(magnet.length <= 8192)
        val key = MagnetIdentity.key(magnet)
        val previous = list()
        val savedTitle = previous.firstOrNull { MagnetIdentity.key(it.magnet) == key }?.title ?: title(magnet)
        val entries = listOf(SavedMagnet(magnet, savedTitle, now)) +
            previous.filter { MagnetIdentity.key(it.magnet) != key }
        write(entries.sortedByDescending { it.addedAt }.take(50))
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
            out.writeInt(-1)
            out.writeInt(entries.size)
            entries.forEach { out.writeUTF(it.magnet); out.writeUTF(it.title); out.writeLong(it.addedAt) }
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
