package com.example.torrentstreamplayer.engine

import org.libtorrent4j.*
import org.libtorrent4j.alerts.*
import java.io.File

internal class MetadataMonitor(private val session: SessionManager,
    private val update: (MetadataDetails) -> Unit) : AlertListener {
    private var details = MetadataDetails(phase = "피어 연결 및 토렌트 정보 수신 중…", source = "네트워크")
    override fun types(): IntArray = intArrayOf(AlertType.METADATA_RECEIVED.swig(),
        AlertType.PEER_CONNECT.swig(), AlertType.PEER_DISCONNECTED.swig())
    override fun alert(alert: Alert<*>) {
        if (alert !is TorrentAlert<*>) return
        runCatching {
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
        session.addListener(this)
        return try { update(details); session.fetchMagnet(magnet, 45, directory) }
        finally { session.removeListener(this) }
    }
}
