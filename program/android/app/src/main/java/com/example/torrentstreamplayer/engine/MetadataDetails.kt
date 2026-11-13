package com.example.torrentstreamplayer.engine

import com.example.torrentstreamplayer.meta.MetadataOrigin
import com.example.torrentstreamplayer.meta.MetadataStage

internal data class MetadataDetails(
    val name: String? = null, val fileCount: Int = 0, val totalSize: Long = 0,
    val pieceCount: Int = 0, val pieceSize: Long = 0, val peers: Int = 0,
    val downloadedBytes: Long = 0, val videoSize: Long = 0,
    val stage: MetadataStage = MetadataStage.RESOLVING, val origin: MetadataOrigin? = null, val elapsedSec: Long = 0
) {
    val hasMetadata: Boolean get() = name != null && fileCount > 0 && pieceCount > 0
    fun connected(count: Int): MetadataDetails = copy(peers = count.coerceAtLeast(0))
    fun received(name: String, files: Int, bytes: Long, pieces: Int, pieceBytes: Long) =
        copy(stage =MetadataStage.RECEIVED, name = name, fileCount = files,
            totalSize = bytes, pieceCount = pieces, pieceSize = pieceBytes)
}
