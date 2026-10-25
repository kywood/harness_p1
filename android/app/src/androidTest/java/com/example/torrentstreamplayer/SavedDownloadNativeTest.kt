package com.example.torrentstreamplayer

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.torrentstreamplayer.engine.*
import org.libtorrent4j.*
import org.libtorrent4j.swig.bitfield
import org.libtorrent4j.swig.libtorrent
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class SavedDownloadNativeTest {
    @Test fun legacyResumeRestoresExactVerifiedSizeAndIgnoresPreallocation() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File.createTempFile("progress-native", ".tmp", context.cacheDir)
        check(root.delete() && root.mkdir())
        try {
            val metadata = ("d4:infod6:lengthi32768e4:name8:clip.mp412:piece lengthi16384e6:pieces40:" +
                "\u0000".repeat(40) + "ee").toByteArray()
            val info = TorrentInfo.bdecode(metadata)
            val params = AddTorrentParams().apply { setTorrentInfo(info) }
            val link = libtorrent.make_magnet_uri(params.swig())
            val key = MagnetIdentity.key(link)
            val downloads = File(root, "downloads").apply { mkdir() }
            val file = File(File(downloads, key), "clip.mp4")
            check(file.parentFile!!.mkdirs())
            file.writeBytes(ByteArray(32768))
            val storage = TorrentCache(File(root, "state"), key, TorrentCacheCipher::encrypt, TorrentCacheCipher::decrypt)
            storage.write("metadata", metadata)
            params.swig().set_have_pieces(bitfield(2, false).apply { set_bit(0) })
            storage.write("resume", AddTorrentParams.writeResumeDataBuf(params))
            val lookup = SavedDownloadLookup(downloads) { storage }
            val entry = SavedMagnet(link, "Clip", 1)
            val restored = lookup.enrich(entry)
            assertEquals(32768L, restored.fileSize)
            assertEquals(16384L, restored.downloadedBytes)
            storage.write("progress", DownloadSnapshot(32768, 32768, file.path).encode())
            assertEquals(32768L, lookup.enrich(entry).downloadedBytes)
            check(file.delete())
            assertEquals(0L, lookup.enrich(entry).downloadedBytes)
        } finally { root.deleteRecursively() }
    }
}
