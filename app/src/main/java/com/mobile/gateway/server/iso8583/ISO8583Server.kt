package com.mobile.gateway.server.iso8583

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.mobile.gateway.extension.hexToAscii
import com.mobile.gateway.extension.hexToByteArray
import com.mobile.gateway.extension.toDateString
import com.mobile.gateway.extension.toHexString
import com.mobile.gateway.server.BasicServer
import com.mobile.gateway.util.DebugPanelManager
import com.mobile.gateway.util.ISO8583Util
import com.mobile.gateway.util.TlvUtil
import org.jpos.core.SimpleConfiguration
import org.jpos.iso.ISOMsg
import org.jpos.iso.channel.NACChannel
import org.jpos.iso.packager.ISO87BPackager
import java.net.ServerSocket

class ISO8583Server(private var host: String, private var port: String) : BasicServer {
    private val jsonFormatter: Gson = GsonBuilder().setPrettyPrinting().create()
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
                val isoReq: ISOMsg? = server?.receive()
                Log.d("ISO8583Server", "Received ISO message: $isoReq")
                eavesdrop(isoReq)

                val isoReply = isoReq?.let { constructReply(it) }
                server?.send(isoReply)
                eavesdrop(isoReply)
                Log.d("ISO8583Server", "Sent ISO message: $isoReply")
            } catch (e: Exception) {
                Log.d("ISO8583Server", "Exception: $e")
                DebugPanelManager.log("ISO8583Server - connection closed\n")
                server?.accept(serverSocket)
            }
        }
    }

    private fun eavesdrop(isoMsg: ISOMsg?) {
        val logMessage = StringBuilder()
        val rawHexData = isoMsg?.pack()?.toHexString()?.uppercase()
        logMessage.append("${if (isoMsg?.isIncoming == true) "Incoming <-- " else "Outgoing --> "} $rawHexData\n")
        val mti = isoMsg?.mti
        logMessage.append("MTI: $mti\n")
        val bitmap = rawHexData?.substring(4, 20)?.let { ISO8583Util.getFieldsFromHex(it) }
        logMessage.append("Bitmap: $bitmap\n")
        bitmap?.forEach {
            val field = Basic8583Field.getByFieldNo(it)
            val fieldNo = it.toString().padStart(2, '0')

            val value = StringBuilder()
            val data = isoMsg.getBytes(it)?.toHexString()?.uppercase() ?: ""
            when (it) {
                62, 63 -> {
                    value.append(data)
                }

                55 -> {
                    value.append(data)
                    val iccData = jsonFormatter.toJson(TlvUtil.decodeTLV(data))
                    value.append("\n$iccData")
                }

                else -> {
                    val asciiValue = data.hexToAscii()
                    value.append(asciiValue)
                }
            }
            logMessage.append("[$fieldNo] ${field?.name}: $value\n")
        }
        DebugPanelManager.log(logMessage.toString())
    }

    private fun constructReply(isoReq: ISOMsg): ISOMsg {
        val isoReply = ISOMsg()
        isoReply.mti = (isoReq.mti.toInt() + 10).toString().padStart(4, '0')
        val replyFields = listOf(4, 11, 12, 13, 24, 37, 38, 39, 41)
        val requestFields = ISO8583Util.getFieldsFromHex(isoReq.pack().toHexString().uppercase().substring(4, 20))
        replyFields.forEach {
            if (requestFields.contains(it)) {
                isoReply.set(it, isoReq.getBytes(it))
            }
            val currentTimestamp = ISO8583Util.getTimeStamp()
            when (it) {
                12, 38 -> {
                    isoReply.set(it, currentTimestamp.toDateString("hhmmss").toByteArray())
                }

                13 -> {
                    isoReply.set(it, currentTimestamp.toDateString("MMdd").toByteArray())
                }

                37 -> {
                    isoReply.set(it, currentTimestamp.toDateString("YYMMddhhmmss").toByteArray())
                }

                39 -> {
                    isoReply.set(it, "00".toByteArray())
                }
            }
        }
        return isoReply
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