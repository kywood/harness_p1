package com.example.torrentstreamplayer.engine

internal data class MetadataDetails(
    val phase: String = "주소 확인 중…", val source: String = "",
    val name: String? = null, val fileCount: Int = 0, val totalSize: Long = 0,
    val pieceCount: Int = 0, val pieceSize: Long = 0, val peers: Int = 0,
    val downloadedBytes: Long = 0, val videoSize: Long = 0
) {
    val hasMetadata: Boolean get() = name != null && fileCount > 0 && pieceCount > 0
    fun connected(count: Int): MetadataDetails = copy(peers = count.coerceAtLeast(0))
    fun received(name: String, files: Int, bytes: Long, pieces: Int, pieceBytes: Long) =
        copy(phase = "토렌트 정보 수신 완료 · 영상 준비 중", name = name, fileCount = files,
            totalSize = bytes, pieceCount = pieces, pieceSize = pieceBytes)
}
