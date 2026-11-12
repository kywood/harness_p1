package com.example.torrentstreamplayer.meta

import java.net.URL

/** Network rules shared by code. Keep [CLEARTEXT_HOSTS] in sync with res/xml/network_security_config.xml. */
internal object NetworkSpec {
    /** The only hosts allowed to use plain HTTP: the in-app stream server on this device. */
    val CLEARTEXT_HOSTS: Set<String> = setOf("127.0.0.1", "localhost")

    /** Longest magnet or `.torrent` address accepted from the user or another app. */
    const val MAX_LINK_CHARS = 8192

    /** Largest `.torrent` file accepted from a link (2 MiB). */
    const val TORRENT_MAX_BYTES = 2 * 1024 * 1024

    /** Connect and read timeout for each `.torrent` download request. */
    const val TORRENT_TIMEOUT_MS = 10_000

    /** HTTP requests made for one `.torrent` link, counting each redirect hop (so at most 5 redirects). */
    const val TORRENT_MAX_REQUESTS = 6

    /** Returns [url] with `http` replaced by `https` unless it points at a local cleartext host. */
    fun secure(url: URL): URL =
        if (!url.protocol.equals("http", ignoreCase = true) || url.host.lowercase() in CLEARTEXT_HOSTS) url
        else URL("https", url.host, if (url.port == 80) -1 else url.port, url.file)
}
