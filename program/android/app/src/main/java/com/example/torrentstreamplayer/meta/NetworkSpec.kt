package com.example.torrentstreamplayer.meta

import java.net.URL

/** Network rules shared by code. Keep [CLEARTEXT_HOSTS] in sync with res/xml/network_security_config.xml. */
internal object NetworkSpec {
    /** The only hosts allowed to use plain HTTP: the in-app stream server on this device. */
    val CLEARTEXT_HOSTS: Set<String> = setOf("127.0.0.1", "localhost")

    /** Returns [url] with `http` replaced by `https` unless it points at a local cleartext host. */
    fun secure(url: URL): URL =
        if (!url.protocol.equals("http", ignoreCase = true) || url.host.lowercase() in CLEARTEXT_HOSTS) url
        else URL("https", url.host, if (url.port == 80) -1 else url.port, url.file)
}
