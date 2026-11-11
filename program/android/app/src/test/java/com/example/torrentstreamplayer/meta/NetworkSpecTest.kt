package com.example.torrentstreamplayer.meta

import com.example.torrentstreamplayer.engine.TorrentInputResolver
import org.junit.Assert.*
import org.junit.Test

class NetworkSpecTest {
    @Test fun torrentDownloadLimitsKeepPreviousValues() {
        assertEquals(2 * 1024 * 1024, NetworkSpec.TORRENT_MAX_BYTES)
        assertEquals(10_000, NetworkSpec.TORRENT_TIMEOUT_MS)
        assertEquals(6, NetworkSpec.TORRENT_MAX_REQUESTS)
    }

    @Test fun resolverLimitIsTheRegisteredValue() {
        assertEquals(NetworkSpec.TORRENT_MAX_BYTES, TorrentInputResolver.MAX_BYTES)
    }
}
