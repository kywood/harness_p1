package com.example.torrentstreamplayer.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ConnectionWatchTest {
    @get:Rule val folder = TemporaryFolder()

    @Test fun lateLossOfAnOldNetworkKeepsTheNewOne() {
        val state = DefaultNetworkState<String>()
        assertEquals(ConnectionType.WIFI, state.changed("wifi", ConnectionType.WIFI))
        assertEquals(ConnectionType.MOBILE, state.changed("cell", ConnectionType.MOBILE))
        assertNull("the old Wi-Fi's onLost arrives after mobile took over", state.lost("wifi"))
        assertEquals(ConnectionType.NONE, state.lost("cell"))
        assertNull("already lost", state.lost("cell"))
    }

    @Test fun shortFlipsAreDroppedAndTheFirstValuePassesAtOnce() = runBlocking {
        val settleMs = 1_000L
        val source = flow {
            emit(ConnectionType.WIFI)
            delay(20)
            emit(ConnectionType.MOBILE)
            delay(20)
            emit(ConnectionType.NONE)
            delay(20)
            emit(ConnectionType.WIFI)
            delay(settleMs * 2)
            emit(ConnectionType.MOBILE)
            delay(settleMs * 2)
        }
        assertEquals(listOf(ConnectionType.WIFI, ConnectionType.MOBILE), source.settled(settleMs).toList())
    }

    @Test fun appStateLivesInItsOwnFolderUnderTheRoot() {
        val root = folder.newFolder()
        val cache = AppStateCache.open(root, "app_settings", { it }, { it })
        cache.write("settings", byteArrayOf(1, 2, 3))
        assertArrayEquals(byteArrayOf(1, 2, 3), AppStateCache.open(root, "app_settings", { it }, { it }).read("settings"))
        assertEquals(listOf("app_settings"), root.list()!!.toList())
        assertNull("another name is another file", AppStateCache.open(root, "magnet_history", { it }, { it }).read("settings"))
    }
}
