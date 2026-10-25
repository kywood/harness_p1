package com.example.torrentstreamplayer.engine

import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

internal data class ResolvedTorrentInput(val magnet: String, val metadata: ByteArray? = null)
internal class TorrentInputResolver(
    private val decode: (ByteArray) -> String,
    private val fetch: (URL) -> ByteArray = ::download
) {
    fun resolve(input: String): ResolvedTorrentInput {
        val value = input.trim()
        require(value.length in 1..8192) { "입력 주소의 길이가 올바르지 않습니다." }
        val uri = URI(value)
        if (uri.scheme.equals("magnet", true)) {
            MagnetIdentity.key(value)
            return ResolvedTorrentInput(value)
        }
        require(uri.scheme in listOf("http", "https") && !uri.host.isNullOrEmpty()) {
            "마그넷 또는 HTTP(S) 토렌트 파일 주소를 입력하세요."
        }
        val bytes = fetch(uri.toURL())
        require(bytes.isNotEmpty() && bytes.size <= MAX_BYTES && bytes[0] == 'd'.code.toByte()) {
            ".torrent 파일이 아닙니다. 파일의 직접 다운로드 주소를 입력하세요."
        }
        val magnet = decode(bytes)
        MagnetIdentity.key(magnet)
        return ResolvedTorrentInput(magnet, bytes)
    }
    companion object {
        const val MAX_BYTES = 2 * 1024 * 1024
        private fun download(initial: URL): ByteArray {
            var url = initial
            repeat(6) {
                require(url.protocol in listOf("http", "https"))
                val connection = url.openConnection() as HttpURLConnection
                try {
                    connection.connectTimeout = 10_000
                    connection.readTimeout = 10_000
                    connection.instanceFollowRedirects = false
                    connection.setRequestProperty("Accept", "application/x-bittorrent, application/octet-stream")
                    val code = connection.responseCode
                    if (code in listOf(301, 302, 303, 307, 308)) {
                        url = URL(url, connection.getHeaderField("Location") ?: error("리디렉션 주소가 없습니다."))
                    } else {
                        check(code in 200..299) { "토렌트 다운로드 실패 (HTTP " + code + ")" }
                        require(connection.contentLengthLong <= MAX_BYTES) { "토렌트 파일이 너무 큽니다." }
                        return connection.inputStream.use { input ->
                            val output = java.io.ByteArrayOutputStream()
                            val buffer = ByteArray(8192)
                            var count = input.read(buffer)
                            while (count >= 0) {
                                require(output.size() + count <= MAX_BYTES) { "토렌트 파일이 너무 큽니다." }
                                output.write(buffer, 0, count); count = input.read(buffer)
                            }
                            output.toByteArray()
                        }
                    }
                } finally { connection.disconnect() }
            }
            error("토렌트 주소의 리디렉션이 너무 많습니다.")
        }
    }
}
