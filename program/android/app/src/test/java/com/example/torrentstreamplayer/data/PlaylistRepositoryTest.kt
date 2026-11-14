package com.example.torrentstreamplayer.data

import com.example.torrentstreamplayer.engine.DownloadOrderPreference
import com.example.torrentstreamplayer.engine.MagnetHistory
import com.example.torrentstreamplayer.engine.MagnetIdentity
import com.example.torrentstreamplayer.engine.TorrentCache
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PlaylistRepositoryTest {
    @get:Rule val folder = TemporaryFolder()

    private fun repository() = PlaylistRepository(
        MagnetHistory(TorrentCache(folder.root, "0".repeat(64), { it }, { it }))) { it.copy(fileSize = 7) }

    private fun magnet(index: Int) = "magnet:?xt=urn:btih:" + index.toString(16).padStart(40, '0')

    @Test fun savedMagnetsAreEnrichedByTheLookup() {
        repository().add(magnet(1))
        assertEquals(7L, repository().savedMagnets().single().fileSize)
    }

    @Test fun addForPlaybackReturnsZeroForANewLinkAndTheSavedPositionAfterwards() {
        assertEquals(0L, repository().addForPlayback(magnet(1)))
        repository().savePosition(magnet(1), 42_000, 120_000)
        assertEquals(42_000L, repository().addForPlayback(magnet(1)))
        assertEquals(1, repository().savedMagnets().size)
    }

    @Test fun negativePositionsAreClampedInsteadOfRejected() {
        repository().add(magnet(1))
        repository().savePosition(magnet(1), -5, -1)
        assertEquals(0L, repository().savedMagnets().single().lastPositionMs)
    }

    @Test fun resetPositionClearsOnlyThatLink() {
        repository().add(magnet(1))
        repository().add(magnet(2))
        repository().savePosition(magnet(1), 42_000, 120_000)
        repository().savePosition(magnet(2), 50_000, 120_000)
        repository().resetPosition(magnet(1))
        val positions = repository().savedMagnets().associate { it.magnet to it.lastPositionMs }
        assertEquals(mapOf(magnet(1) to 0L, magnet(2) to 50_000L), positions)
    }

    @Test fun downloadOrdersListOnlyVideosWithTheirOwnValue() {
        repository().add(magnet(1))
        repository().add(magnet(2))
        repository().setDownloadOrder(magnet(1), DownloadOrderPreference.FASTEST)
        assertEquals(mapOf(MagnetIdentity.key(magnet(1)) to DownloadOrderPreference.FASTEST), repository().downloadOrders())
        assertEquals(DownloadOrderPreference.FASTEST,
            repository().savedMagnets().first { it.magnet == magnet(1) }.downloadOrder)
        repository().setDownloadOrder(magnet(1), null)
        assertEquals(emptyMap<String, DownloadOrderPreference>(), repository().downloadOrders())
    }

    @Test fun aFailedSaveThrowsAndKeepsTheSavedDownloadOrder() {
        repository().add(magnet(1))
        repository().setDownloadOrder(magnet(1), DownloadOrderPreference.FASTEST)
        val failing = PlaylistRepository(MagnetHistory(TorrentCache(folder.root, "0".repeat(64),
            { throw java.io.IOException("disk full") }, { it }))) { it }
        val failure = runCatching { failing.setDownloadOrder(magnet(1), DownloadOrderPreference.FROM_START) }
        assertEquals(true, failure.isFailure)
        assertEquals(DownloadOrderPreference.FASTEST, failing.savedMagnets().single().downloadOrder)
        assertEquals(DownloadOrderPreference.FASTEST, repository().savedMagnets().single().downloadOrder)
    }

    @Test fun keysListThePlaylistWithoutTheLookup() {
        val repository = PlaylistRepository(MagnetHistory(TorrentCache(folder.root, "0".repeat(64), { it }, { it }))) {
            error("keys must not read download details")
        }
        repository.add(magnet(1))
        assertEquals(setOf(MagnetIdentity.key(magnet(1))), repository.keys())
    }

    @Test fun findReturnsTheEntryWithTheSameKeyWithDownloadDetails() {
        repository().add(magnet(1) + "&dn=Old")
        repository().savePosition(magnet(1), 42_000, 120_000)
        val found = repository().find(MagnetIdentity.key(magnet(1) + "&tr=other"))
        assertEquals(42_000L, found?.lastPositionMs)
        assertEquals(7L, found?.fileSize)
        assertEquals(null, repository().find(MagnetIdentity.key(magnet(2))))
    }

    @Test fun titleMarkPlayedAndDeleteGoThroughToHistory() {
        repository().add(magnet(1))
        repository().updateTitle(magnet(1), "영상")
        repository().markPlayed(magnet(1))
        val saved = repository().savedMagnets().single()
        assertEquals("영상", saved.title)
        assertEquals(true, saved.lastPlayedAt > 0)
        repository().delete(magnet(1))
        assertEquals(emptyList<Any>(), repository().savedMagnets())
    }
}
