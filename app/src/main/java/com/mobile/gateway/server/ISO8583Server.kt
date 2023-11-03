package com.mobile.gateway.server

import android.util.Log
import com.mobile.gateway.extension.hexToByteArray
import org.jpos.core.SimpleConfiguration
import org.jpos.iso.ISOMsg
import org.jpos.iso.channel.NACChannel
import org.jpos.iso.packager.ISO87BPackager
import java.net.ServerSocket

class ISO8583Server(private var host: String, private var port: String) : BasicServer {
    private var server: NACChannel? = null
    override fun getInstance(): Any? {
        return server
    }

    override fun startServer(wait: Boolean) {
        server = createChannel()
        server?.configuration = getChannelConfig()

        // Create the ServerSocket
        val serverSocket = ServerSocket(port.toInt())
        server?.accept(serverSocket)

        while (true) {
            try {
                val isoMsg: ISOMsg? = server?.receive()
                Log.d("ISO8583Server", "Received ISO message: $isoMsg")

                // Handle the ISO message here
                // ...
            } catch (e: Exception) {
                Log.d("ISO8583Server", "Exception: $e")
            }
        }
    }

    private fun createChannel(): NACChannel {
        return NACChannel(
            ISO87BPackager(),
            getDefaultTPDUHeader()
        )
    }

    private fun getDefaultTPDUHeader(): ByteArray {
        val destination = "98".padStart(4, '0')
        val originator = "0000"
        val tpdu = "60$destination$originator"
        return tpdu.hexToByteArray()
    }

    private fun getChannelConfig(): SimpleConfiguration {
        val config = SimpleConfiguration()
        config.put("host", host)
        config.put("port", port)
        return config
    }

    override fun stopServer() {
        server?.disconnect()
    }
}