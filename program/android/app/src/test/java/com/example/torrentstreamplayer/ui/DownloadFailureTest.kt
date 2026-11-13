package com.example.torrentstreamplayer.ui

import com.example.torrentstreamplayer.engine.InvalidMagnetException
import com.example.torrentstreamplayer.meta.TorrentFailure
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.UnknownHostException

class DownloadFailureTest {
    @Test fun appFailureKeepsItsItemAndArguments() {
        assertEquals(DownloadFailure.Registered(TorrentFailure.TORRENT_HTTP, listOf(404)),
            DownloadFailure.of(TorrentFailure.TORRENT_HTTP.blocked(404)))
    }

    @Test fun linkAndNetworkErrorsUseTheirRegisteredItems() {
        assertEquals(DownloadFailure.Registered(TorrentFailure.INVALID_MAGNET), DownloadFailure.of(InvalidMagnetException()))
        assertEquals(DownloadFailure.Registered(TorrentFailure.LINK_GENERIC),
            DownloadFailure.of(UnknownHostException("tracker.example.com")))
    }

    @Test fun otherExceptionsNeverKeepTheirMessage() {
        assertEquals(DownloadFailure.StartFailed, DownloadFailure.of(IllegalStateException("Failed requirement.")))
    }
}
