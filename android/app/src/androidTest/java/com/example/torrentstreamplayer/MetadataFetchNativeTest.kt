package com.example.torrentstreamplayer

import androidx.test.platform.app.InstrumentationRegistry
import com.example.torrentstreamplayer.engine.MetadataFetch
import org.junit.Assert.*
import org.junit.Test
import org.libtorrent4j.*
import org.libtorrent4j.swig.libtorrent
import java.io.File

class MetadataFetchNativeTest {
    @Test fun firstUncachedMagnetReceivesMetadataFromPeerWithoutManualRestart() = runTest { session, root, _, _ ->
        val info = ("d6:lengthi32768e4:name9:fresh.mkv12:piece lengthi16384e6:pieces40:" + "\u0000".repeat(40) + "e").toByteArray()
        val hash = java.security.MessageDigest.getInstance("SHA-1").digest(info).joinToString("") { "%02x".format(it) }
        MetadataTestPeer(info).use { peer ->
            val peerErrors = java.lang.StringBuffer()
            session.addListener(object : org.libtorrent4j.AlertListener {
                override fun types() = intArrayOf(org.libtorrent4j.alerts.AlertType.PEER_BLOCKED.swig(),
                    org.libtorrent4j.alerts.AlertType.PEER_ERROR.swig())
                override fun alert(alert: org.libtorrent4j.alerts.Alert<*>) { peerErrors.append(alert.message()).append("; ") }
            })
            var nextConnect = 0L
            var diagnostics = ""
            val bytes = MetadataFetch.fetch(session, "magnet:?xt=urn:btih:" + hash, root, 10000) { handle, elapsed ->
                diagnostics = "state=${handle.status().state()} flags=${handle.getFlags().to_int()} peers=${handle.status().numPeers()}"
                if (elapsed >= nextConnect) { nextConnect = elapsed + 1000
                    handle.swig().connect_peer(TcpEndpoint("127.0.0.1", peer.port).swig()) }
            }
            assertNotNull("First metadata connection must succeed; peer=${peer.state}; $diagnostics; $peerErrors", bytes)
            assertEquals("fresh.mkv", TorrentInfo.bdecode(bytes!!).name())
        }
    }
    private fun await(check: () -> Boolean) {
        val deadline = System.nanoTime() + 5_000_000_000L
        while (!check()) { check(System.nanoTime() < deadline); Thread.sleep(25) }
    }
    private fun runTest(test: (SessionManager, File, TorrentInfo, String) -> Unit) {
        val root = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
            "metadata-native-" + System.nanoTime()).apply { mkdirs() }
        val name = "metadata-test-" + System.nanoTime() + ".mkv"
        val bytes = ("d4:infod6:lengthi32768e4:name" + name.length + ":" + name +
            "12:piece lengthi16384e6:pieces40:" + "\u0000".repeat(40) + "7:privatei1eee").toByteArray()
        val info = TorrentInfo.bdecode(bytes)
        val params = AddTorrentParams().apply { setTorrentInfo(info) }
        val magnet = libtorrent.make_magnet_uri(params.swig())
        val session = SessionManager()
        val settings = SettingsPack().apply {
            setBoolean(org.libtorrent4j.swig.settings_pack.bool_types.enable_outgoing_utp.swigValue(), false)
            setInteger(org.libtorrent4j.swig.settings_pack.int_types.out_enc_policy.swigValue(),
                org.libtorrent4j.swig.settings_pack.enc_policy.pe_disabled.swigValue())
        }
        session.start(SessionParams(settings))
        try { test(session, root, info, magnet) }
        finally { session.stop(); root.deleteRecursively() }
    }
    @Test fun existingMetadataIsReturnedWithoutResumeDataAndExistingDownloadIsNotRemoved() = runTest { session, root, info, magnet ->
        session.download(info, root)
        val hash = AddTorrentParams.parseMagnetUri(magnet).infoHashes.best
        await { session.find(hash)?.isValid == true }
        val bytes = MetadataFetch.fetch(session,
            magnet + "&tr=https%3A%2F%2Ftracker.example.invalid%2Fannounce", root, 2000) { _, _ -> }
        assertNotNull(bytes)
        val decoded = TorrentInfo.bdecode(bytes!!)
        assertEquals(info.name(), decoded.name())
        assertEquals(info.numPieces(), decoded.numPieces())
        assertTrue(String(bytes, Charsets.ISO_8859_1).contains("tracker.example.invalid"))
        assertTrue(session.find(hash)!!.isValid)
    }
    @Test fun timeoutRemovesOnlyItsOwnTemporaryMetadataHandle() = runTest { session, root, info, magnet ->
        session.download(info, root)
        val existing = AddTorrentParams.parseMagnetUri(magnet).infoHashes.best
        await { session.find(existing)?.isValid == true }
        val missing = "magnet:?xt=urn:btih:" + "b".repeat(40)
        val hash = AddTorrentParams.parseMagnetUri(missing).infoHashes.best
        var updates = 0
        assertNull(MetadataFetch.fetch(session, missing, root, 500) { _, _ -> updates++ })
        assertTrue(updates >= 2)
        await { session.find(hash)?.isValid != true }
        assertTrue(session.find(existing)!!.isValid)
    }
    @Test fun cancellingFirstConnectionRemovesTemporaryHandlePromptly() = runTest { session, root, _, _ ->
        val magnet = "magnet:?xt=urn:btih:" + "c".repeat(40)
        val hash = AddTorrentParams.parseMagnetUri(magnet).infoHashes.best
        val interrupted = java.util.concurrent.atomic.AtomicBoolean()
        val thread = Thread {
            try { MetadataFetch.fetch(session, magnet, root) { _, _ -> } }
            catch (_: InterruptedException) { interrupted.set(true) }
        }
        thread.start()
        try {
            await { session.find(hash)?.isValid == true }
            thread.interrupt(); thread.join(5000)
            assertFalse(thread.isAlive)
            assertTrue(interrupted.get())
            await { session.find(hash)?.isValid != true }
        } finally { thread.interrupt(); thread.join(5000) }
    }
}
