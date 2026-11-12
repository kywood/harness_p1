package com.example.torrentstreamplayer.engine

import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import com.example.torrentstreamplayer.meta.NetworkSpec

internal data class ResolvedTorrentInput(val magnet: String, val metadata: ByteArray? = null)
internal class TorrentInputResolver(
    private val decode: (ByteArray) -> String,
    private val fetch: (URL) -> ByteArray = ::download
) {
    fun resolve(input: String): ResolvedTorrentInput {
        val value = input.trim()
        require(value.length in 1..NetworkSpec.MAX_LINK_CHARS) { "입력 주소의 길이가 올바르지 않습니다." }
        if (value.startsWith("magnet:", true)) return ResolvedTorrentInput(value.also(MagnetIdentity::key))
        val uri = webAddress(value)
        require(uri != null) { "마그넷 또는 HTTP(S) 토렌트 파일 주소를 입력하세요." }
        val target = NetworkSpec.secure(uri.toURL())
        val bytes = try { fetch(target) } catch (e: java.io.IOException) {
            if (target.protocol != uri.scheme) throw IllegalArgumentException("보안 연결(HTTPS)을 지원하지 않는 주소입니다. https:// 주소를 입력하세요.", e)
            throw e
        }
        require(bytes.isNotEmpty() && bytes.size <= MAX_BYTES && bytes[0] == 'd'.code.toByte()) {
            ".torrent 파일이 아닙니다. 파일의 직접 다운로드 주소를 입력하세요."
        }
        val magnet = try { decode(bytes) } catch (e: Exception) { throw InvalidTorrentFileException(e) }
        MagnetIdentity.key(magnet)
        return ResolvedTorrentInput(magnet, bytes)
    }
    /** The downloaded bytes are not a readable torrent. The UI shows its own localized text for this case. */
    class InvalidTorrentFileException(cause: Throwable) : IllegalArgumentException("Unreadable torrent file", cause)
    companion object {
        const val MAX_BYTES = NetworkSpec.TORRENT_MAX_BYTES
        /** [value] as an http(s) address with a host, the only kind [resolve] downloads; otherwise null. */
        fun webAddress(value: String): URI? = runCatching { URI(value) }.getOrNull()
            ?.takeIf { it.scheme in listOf("http", "https") && !it.host.isNullOrEmpty() }
        private fun download(initial: URL): ByteArray {
            var url = initial
            repeat(NetworkSpec.TORRENT_MAX_REQUESTS) {
                require(url.protocol in listOf("http", "https"))
                val connection = url.openConnection() as HttpURLConnection
                try {
                    connection.connectTimeout = NetworkSpec.TORRENT_TIMEOUT_MS
                    connection.readTimeout = NetworkSpec.TORRENT_TIMEOUT_MS
                    connection.instanceFollowRedirects = false
                    connection.setRequestProperty("Accept", "application/x-bittorrent, application/octet-stream")
                    val code = connection.responseCode
                    if (code in listOf(301, 302, 303, 307, 308)) {
                        url = NetworkSpec.secure(URL(url, connection.getHeaderField("Location") ?: error("리디렉션 주소가 없습니다.")))
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
