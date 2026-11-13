package com.example.torrentstreamplayer.ui

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistWriteGateTest {
    @Test fun aRecordQueuedBehindADeleteIsSkipped() = runBlocking {
        val alive = mutableSetOf("k")
        val playlist = mutableSetOf<String>()
        val gate = PlaylistWriteGate { it in alive }
        val deleting = CompletableDeferred<Unit>()
        val finishDelete = CompletableDeferred<Unit>()

        val delete = async {
            gate.delete {
                alive -= "k"
                deleting.complete(Unit)
                finishDelete.await()
                playlist -= "k"
            }
        }
        deleting.await()
        val record = async { gate.recordIfAlive("k") { playlist += "k" } }
        yield()
        finishDelete.complete(Unit)
        delete.await()

        assertFalse("the late write sees the stopped task", record.await())
        assertEquals("the deleted card does not come back", emptySet<String>(), playlist)
    }

    @Test fun aRecordBeforeTheDeleteIsDeletedAfterIt() = runBlocking {
        val alive = mutableSetOf("k")
        val playlist = mutableListOf<String>()
        val gate = PlaylistWriteGate { it in alive }

        assertTrue(gate.recordIfAlive("k") { playlist += "add" })
        gate.delete {
            alive -= "k"
            playlist += "delete"
        }

        assertEquals(listOf("add", "delete"), playlist)
    }

    @Test fun aStoppedTaskIsNotRecorded() = runBlocking {
        val gate = PlaylistWriteGate { false }
        var written = false
        assertFalse(gate.recordIfAlive("k") { written = true })
        assertFalse(written)
    }
}
