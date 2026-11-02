package com.example.torrentstreamplayer

import java.io.*
import java.net.*

internal class MetadataTestPeer(private val info: ByteArray) : AutoCloseable {
    private val server = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))
    val port: Int get() = server.localPort
    @Volatile private var connection: Socket? = null
    @Volatile var state = "listening"
    private val worker = Thread {
        try {
            server.accept().use { socket ->
                connection = socket
                state = "connected"
                socket.soTimeout = 10000
                val input = DataInputStream(socket.getInputStream())
                val output = DataOutputStream(socket.getOutputStream())
                val handshake = ByteArray(68).also(input::readFully)
                state = "handshake received"
                handshake[25] = 16
                ByteArray(20) { 'P'.code.toByte() }.copyInto(handshake, 48)
                output.write(handshake)
                fun send(extension: Int, bytes: ByteArray) {
                    output.writeInt(bytes.size + 2)
                    output.writeByte(20); output.writeByte(extension); output.write(bytes); output.flush()
                }
                send(0, ("d1:md11:ut_metadatai1ee13:metadata_sizei" + info.size + "ee").toByteArray())
                var clientExtension = 1
                while (!socket.isClosed) {
                    val length = input.readInt()
                    if (length == 0) continue
                    require(length in 1..65536)
                    val message = ByteArray(length).also(input::readFully)
                    state = "message id=${message[0]} length=$length"
                    if (message.size < 2 || message[0].toInt() != 20) continue
                    if (message[1].toInt() == 0) {
                        Regex("11:ut_metadatai([0-9]+)e").find(String(message, Charsets.ISO_8859_1))
                            ?.groupValues?.get(1)?.toInt()?.let { clientExtension = it }
                    } else {
                        send(clientExtension, ("d8:msg_typei1e5:piecei0e10:total_sizei" +
                            info.size + "ee").toByteArray() + info)
                    }
                }
            }
        } catch (error: Exception) { state = "closed: " + error.message }
    }.apply { isDaemon = true; start() }
    override fun close() {
        connection?.close(); server.close(); worker.join(2000)
    }
}
