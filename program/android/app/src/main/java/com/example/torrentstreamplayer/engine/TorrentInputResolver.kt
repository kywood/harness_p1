package com.example.torrentstreamplayer.engine

import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import com.example.torrentstreamplayer.meta.NetworkSpec
import com.example.torrentstreamplayer.meta.TorrentFailure

internal data class ResolvedTorrentInput(val magnet: String, val metadata: ByteArray? = null)
internal class TorrentInputResolver(
    private val decode: (ByteArray) -> String,
    private val fetch: (URL) -> ByteArray = ::download
) {
    fun resolve(input: String): ResolvedTorrentInput {
        val value = input.trim()
        if (value.length !in 1..NetworkSpec.MAX_LINK_CHARS) throw TorrentFailure.INPUT_LENGTH.invalid()
        if (value.startsWith("magnet:", true)) return ResolvedTorrentInput(value.also(MagnetIdentity::key))
        val uri = webAddress(value)
        if (uri == null) throw TorrentFailure.INPUT_UNSUPPORTED.invalid()
        val target = NetworkSpec.secure(uri.toURL())
        val bytes = try { fetch(target) } catch (e: java.io.IOException) {
            if (target.protocol != uri.scheme) throw TorrentFailure.INPUT_HTTPS.invalid(e)
            throw e
        }
        if (bytes.isEmpty() || bytes.size > MAX_BYTES || bytes[0] != 'd'.code.toByte()) throw TorrentFailure.NOT_TORRENT.invalid()
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
                        url = NetworkSpec.secure(URL(url, connection.getHeaderField("Location") ?: throw TorrentFailure.REDIRECT_MISSING.blocked()))
                    } else {
                        if (code !in 200..299) throw TorrentFailure.TORRENT_HTTP.blocked(code)
                        if (connection.contentLengthLong > MAX_BYTES) throw TorrentFailure.TORRENT_TOO_LARGE.invalid()
                        return connection.inputStream.use { input ->
                            val output = java.io.ByteArrayOutputStream()
                            val buffer = ByteArray(8192)
                            var count = input.read(buffer)
                            while (count >= 0) {
                                if (output.size() + count > MAX_BYTES) throw TorrentFailure.TORRENT_TOO_LARGE.invalid()
                                output.write(buffer, 0, count); count = input.read(buffer)
                            }
                            output.toByteArray()
                        }
                    }
                } finally { connection.disconnect() }
            }
            throw TorrentFailure.TOO_MANY_REDIRECTS.blocked()
        }
    }
}
