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
    @Test fun malformedInputIsRejectedWithUserTextNotParserText() {
        val resolver = TorrentInputResolver({ magnet }, { error("Network must not be used") })
        assertThrows(InvalidMagnetException::class.java) { resolver.resolve("$magnet&dn=[Group] Title") }
        val url = assertThrows(IllegalArgumentException::class.java) { resolver.resolve("https://exa mple.test/a b") }
        assertEquals("마그넷 또는 HTTP(S) 토렌트 파일 주소를 입력하세요.", url.message)
    }
    @Test fun unreadableTorrentBytesAreReportedAsTorrentFileFailure() {
        val failure = assertThrows(TorrentInputResolver.InvalidTorrentFileException::class.java) {
            TorrentInputResolver({ error("bad bencode") }, { "d4:info".toByteArray() }).resolve("https://example.test/a")
        }
        assertEquals("bad bencode", failure.cause?.message)
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
    @Test fun externalHttpLinkIsFetchedOverHttps() {
        val requested = mutableListOf<String>()
        val bytes = "d4:infodee".toByteArray()
        val resolver = TorrentInputResolver({ magnet }, { requested += it.toString(); bytes })
        resolver.resolve("http://example.test/a.torrent")
        resolver.resolve("http://example.test:80/b.torrent")
        resolver.resolve("http://example.test:8080/c.torrent")
        resolver.resolve("https://example.test/d.torrent")
        assertEquals(listOf("https://example.test/a.torrent", "https://example.test/b.torrent",
            "https://example.test:8080/c.torrent", "https://example.test/d.torrent"), requested)
    }
    @Test fun localStreamHostKeepsPlainHttp() {
        var requested = ""
        TorrentInputResolver({ magnet }, { requested = it.toString(); "d4:infodee".toByteArray() })
            .resolve("http://127.0.0.1:9000/a.torrent")
        assertEquals("http://127.0.0.1:9000/a.torrent", requested)
    }
    @Test fun upgradedLinkWithoutHttpsExplainsWhatToEnter() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            TorrentInputResolver({ magnet }, { throw java.net.ConnectException("refused") })
                .resolve("http://example.test/a.torrent")
        }
        assertTrue(error.message!!.contains("HTTPS"))
    }
    @Test fun networkSecurityConfigAllowsExactlyTheRegisteredCleartextHosts() {
        val xml = java.io.File("src/main/res/xml/network_security_config.xml").readText()
        val hosts = Regex("<domain[^>]*>([^<]+)</domain>").findAll(xml).map { it.groupValues[1].trim() }.toSet()
        assertEquals(com.example.torrentstreamplayer.meta.NetworkSpec.CLEARTEXT_HOSTS, hosts)
        assertTrue(xml.contains("<base-config cleartextTrafficPermitted=\"false\""))
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
