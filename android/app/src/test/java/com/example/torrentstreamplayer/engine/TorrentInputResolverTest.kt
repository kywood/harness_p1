package com.example.torrentstreamplayer.engine
import org.junit.Assert.*
import org.junit.Test
import java.net.InetSocketAddress
import com.sun.net.httpserver.HttpServer

class TorrentInputResolverTest {
    private val magnet = "magnet:?xt=urn:btih:" + "a".repeat(40)
    @Test fun magnetBypassesTorrentDownloadAndConversion() {
        val result = TorrentInputResolver({ error("No conversion") }, { error("No network") }).resolve(magnet)
        assertEquals(magnet, result.magnet)
        assertNull(result.metadata)
    }
    @Test fun directTorrentBytesAreConvertedAndRetainedForCache() {
        val bytes = "d4:infodee".toByteArray()
        val result = TorrentInputResolver({ assertArrayEquals(bytes, it); magnet }, { bytes })
            .resolve("https://example.test/download?id=123")
        assertEquals(magnet, result.magnet)
        assertArrayEquals(bytes, result.metadata)
    }
    @Test fun htmlPageAndOversizedDownloadAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            TorrentInputResolver({ magnet }, { "<html>Login</html>".toByteArray() })
                .resolve("https://example.test/file.torrent")
        }
        assertThrows(IllegalArgumentException::class.java) {
            TorrentInputResolver({ magnet }, { ByteArray(TorrentInputResolver.MAX_BYTES + 1) })
                .resolve("https://example.test/file.torrent")
        }
    }
    @Test fun unsupportedSchemesAndInvalidMagnetAreRejected() {
        listOf("file:///tmp/a.torrent", "ftp://example.test/a", "magnet:?xt=invalid").forEach { input ->
            assertThrows(Exception::class.java) {
                TorrentInputResolver({ magnet }, { error("Network must not be used") }).resolve(input)
            }
        }
    }
    @Test fun followsHttpRedirectThenDownloadsTorrent() {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val bytes = "d4:infodee".toByteArray()
        server.createContext("/redirect") { it.responseHeaders.add("Location", "/file"); it.sendResponseHeaders(302, -1); it.close() }
        server.createContext("/file") { it.sendResponseHeaders(200, bytes.size.toLong()); it.responseBody.use { out -> out.write(bytes) }; it.close() }
        server.start()
        try {
            val result = TorrentInputResolver({ magnet }).resolve("http://127.0.0.1:" + server.address.port + "/redirect")
            assertArrayEquals(bytes, result.metadata)
        } finally { server.stop(0) }
    }
    @Test fun httpErrorIsReportedInsteadOfParsingErrorPage() {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/missing") { it.sendResponseHeaders(404, -1); it.close() }
        server.start()
        try {
            assertThrows(IllegalStateException::class.java) {
                TorrentInputResolver({ magnet }).resolve("http://127.0.0.1:" + server.address.port + "/missing")
            }
        } finally { server.stop(0) }
    }
}
