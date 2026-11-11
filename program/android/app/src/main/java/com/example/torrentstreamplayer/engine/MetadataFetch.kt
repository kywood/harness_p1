package com.example.torrentstreamplayer.engine

import org.libtorrent4j.*
import org.libtorrent4j.swig.libtorrent
import java.io.File

internal object MetadataFetch {
    private val adding = Any()
    fun fetch(session: SessionManager, magnet: String, directory: File, timeoutMillis: Long = 120_000,
        progress: (TorrentHandle, Long) -> Unit): ByteArray? {
        val params = AddTorrentParams.parseMagnetUri(magnet)
        val hash = params.infoHashes.best
        var owned = false
        val handle = synchronized(adding) {
            session.find(hash)?.takeIf { it.isValid } ?: run {
                params.savePath = directory.absolutePath
                params.setFlags(params.getFlags().and_(TorrentFlags.AUTO_MANAGED.inv())
                    .and_(TorrentFlags.PAUSED.inv()).or_(TorrentFlags.UPLOAD_MODE)
                    .or_(TorrentFlags.STOP_WHEN_READY))
                val error = ErrorCode(org.libtorrent4j.swig.error_code())
                SessionHandle(session.swig()).addTorrent(params, error).also {
                    check(error.value == 0 && it.isValid) { "메타데이터 연결 실패: " + error.message }
                    owned = true
                    it.resume()
                }
            }
        }
        val discovery = MetadataDiscovery()
        return try {
            MetadataWait.await(timeoutMillis) { elapsed ->
                check(session.isRunning && handle.isValid) { "메타데이터 연결이 종료되었습니다." }
                progress(handle, elapsed)
                if (owned && handle.torrentFile() == null && discovery.shouldRetry(elapsed, handle.status().numPeers())) {
                    android.util.Log.i("TorrentMetadata", "No peers at ${elapsed}ms; refreshing discovery on same handle")
                    handle.forceDHTAnnounce()
                    handle.forceReannounce()
                }
                handle.torrentFile()?.takeIf { it.isValid }?.let { info ->
                    android.util.Log.i("TorrentMetadata", "Metadata ready at ${elapsed}ms; pieces=${info.numPieces()}")
                    val data = params.apply { setTorrentInfo(info) }
                    Vectors.byte_vector2bytes(libtorrent.write_torrent_file_buf_ex(data.swig()))
                }
            }
        } finally {
            if (owned && session.isRunning) runCatching { if (handle.isValid) session.remove(handle) }
        }
    }
}
