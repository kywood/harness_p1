package com.example.torrentstreamplayer.engine

import com.example.torrentstreamplayer.meta.MetadataOrigin
import com.example.torrentstreamplayer.meta.MetadataStage
import org.libtorrent4j.*
import org.libtorrent4j.alerts.*
import java.io.File

internal class MetadataMonitor(private val session: SessionManager,
    private val update: (MetadataDetails) -> Unit) : AlertListener {
    private var details = MetadataDetails(stage = MetadataStage.CONNECTING, origin = MetadataOrigin.NETWORK)
    @Volatile private var target: Sha1Hash? = null
    override fun types(): IntArray = intArrayOf(AlertType.METADATA_RECEIVED.swig(),
        AlertType.PEER_CONNECT.swig(), AlertType.PEER_DISCONNECTED.swig())
    override fun alert(alert: Alert<*>) {
        if (alert !is TorrentAlert<*>) return
        runCatching {
            if (!alert.handle().isValid || alert.handle().infoHash() != target) return
            details = details.connected(alert.handle().status().numPeers())
            if (alert is MetadataReceivedAlert) {
                val info = alert.handle().torrentFile() ?: return
                details = details.received(info.name(), info.files().numFiles(),
                    info.totalSize(), info.numPieces(), info.pieceLength().toLong())
            }
            update(details)
        }
    }
    fun fetch(magnet: String, directory: File): ByteArray? {
        target = AddTorrentParams.parseMagnetUri(magnet).infoHashes.best
        session.addListener(this)
        return try { MetadataFetch.fetch(session, magnet, directory) { handle, elapsed ->
            details = details.connected(handle.status().numPeers())
            handle.torrentFile()?.takeIf { it.isValid }?.let { info ->
                details = details.received(info.name(), info.files().numFiles(),
                    info.totalSize(), info.numPieces(), info.pieceLength().toLong())
            }
            if (!details.hasMetadata) details = details.copy(stage = MetadataStage.SEARCHING, elapsedSec = elapsed / 1000)
            update(details)
        } }
        finally { session.removeListener(this) }
    }
}
