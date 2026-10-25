package com.example.torrentstreamplayer.engine

internal object MetadataStartup {
    fun <S> load(cached: ByteArray?, start: () -> S, settle: () -> Unit,
        fetch: (S) -> ByteArray?): Pair<ByteArray?, S?> {
        if (cached != null) return cached to null
        val session = start()
        settle()
        return fetch(session) to session
    }
}
