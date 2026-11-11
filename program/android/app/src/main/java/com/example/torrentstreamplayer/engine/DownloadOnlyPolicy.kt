package com.example.torrentstreamplayer.engine

internal object DownloadOnlyPolicy {
    fun <K> settings(choker: K, slots: K, fastPieces: K, fixedChoker: Int): Map<K, Int> =
        mapOf(choker to fixedChoker, slots to 0, fastPieces to 0)
    fun assignPeerClasses(assign: (String, String, Long) -> Unit) {
        assign("0.0.0.0", "255.255.255.255", 1L)
        assign("::", "ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff", 1L)
    }
}
