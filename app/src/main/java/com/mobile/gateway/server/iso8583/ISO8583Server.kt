package com.mobile.gateway.server.iso8583

import android.content.Context
import android.util.Log
import com.mobile.gateway.extension.hexToAscii
import com.mobile.gateway.extension.hexToByteArray
import com.mobile.gateway.extension.toDateString
import com.mobile.gateway.extension.toHexString
import com.mobile.gateway.server.BasicServer
import com.mobile.gateway.util.DebugPanelManager
import com.mobile.gateway.util.ISO8583Util
import com.mobile.gateway.util.PreferencesUtil
import com.mobile.gateway.util.TlvUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jpos.core.SimpleConfiguration
import org.jpos.iso.ISOMsg
import org.jpos.iso.channel.NACChannel
import org.jpos.iso.packager.ISO87BPackager
import java.net.ServerSocket
import java.net.SocketException

class ISO8583Server(context: Context, private var serverConfig: ISO8583ServerConfig) : BasicServer<NACChannel>(context) {
    override fun startServer(wait: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                server = createChannel()
                server?.configuration = getChannelConfig(serverConfig.host, serverConfig.port)

                // Create the ServerSocket
                serverSocket = ServerSocket(serverConfig.port.toInt())
                DebugPanelManager.log("[ISO8583] Server IP: ${serverConfig.host} Port: ${serverConfig.port}")

                val redirectHost = serverConfig.redirectDestination?.substringAfter("//")?.substringBeforeLast(':')
                val redirectPort = serverConfig.redirectDestination?.substringAfterLast(':')
                val isProxyMode = serverConfig.isProxy && !redirectHost.isNullOrEmpty() && !redirectPort.isNullOrEmpty()
                if (isProxyMode) DebugPanelManager.log("[ISO8583] Redirect to host: $redirectHost, port: $redirectPort")
                DebugPanelManager.log("-".repeat(50))

                server?.accept(serverSocket) // server start listening request

                while (serverSocket != null) {
                    try {
                        val isoReq: ISOMsg? = server?.receive()
                        eavesdrop(isoReq)

                        // two option here:
                        // 1. construct iso8583 reply and send
                        // 2. route iso8583 request as a proxy to a configurable destination (Default: PyHostSim)
                        if (isProxyMode) {
                            val serverB = createChannel()
                            serverB.configuration = getChannelConfig(redirectHost!!, redirectPort!!) // IDE cannot cast boolean -> force not null here
                            serverB.connect()
                            serverB.send(isoReq)

                            val incoming = try {
                                serverB.receive()
                            } catch (e: Exception) {
                                null
                            }
                            server?.send(incoming)
                            eavesdrop(incoming)
                        } else {
                            val isoReply = isoReq?.let { constructReply(it) }
                            server?.send(isoReply)
                            eavesdrop(isoReply)
                        }
                    } catch (e: Exception) {
                        Log.d("ISO8583Server", "[In while loop] Exception: $e")
//                        DebugPanelManager.log("[ISO8583] Server - connection closed\n")
                        server?.accept(serverSocket)
                    }

                }
            } catch (socketException: SocketException) {
                DebugPanelManager.log("-".repeat(50))
                DebugPanelManager.log("[ISO8583] Server - ${socketException.message}")
            } catch (ex: Exception) {
                DebugPanelManager.log("[ISO8583] Server - Exception: $ex")
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

        val serverProfile = PreferencesUtil.getISO8583ServerProfile(context)
        Log.d("ISO8583Reply", "$serverProfile")

        // check any match profile
        val matchProfile = serverProfile.profiles.find { responseConfig ->
            responseConfig.filters?.entries?.all {
                if (it.key.contains("mti", ignoreCase = true)) {
                    Regex(it.value).matches(isoReq.mti)
                } else {
                    val fieldNo = it.key.drop(2).toInt() //"DE22" -> 22
                    val value = isoReq.getBytes(fieldNo).toHexString().hexToAscii()
                    Log.d("ISO8583Reply", "check {${it.key}, ${it.value}}  [$fieldNo]=$value")
                    Regex(it.value).matches(isoReq.getBytes(fieldNo).toHexString().hexToAscii())
                }
            } == true
        }

        // get default profile
        val defaultProfile = serverProfile.profiles.find { responseConfig ->
            responseConfig.filters == null
        } ?: throw Exception("Invalid Server Config")

        val replyProfile = (matchProfile ?: defaultProfile)
        Log.d("ISO8583Reply", "replyProfile: ${jsonFormatter.toJson(replyProfile)}")
        val replyFields = replyProfile.fields
        Log.d("ISO8583Reply", "replyFields: $replyFields")
        val requestFields = ISO8583Util.getFieldsFromHex(isoReq.pack().toHexString().uppercase().substring(4, 20))
        replyFields.forEach { fieldNo ->
            if (requestFields.contains(fieldNo)) {
                isoReply.set(fieldNo, isoReq.getBytes(fieldNo))
            }

            if (replyProfile.data?.contains("DE$fieldNo") == true) {
                val value = replyProfile.data["DE$fieldNo"]
                Log.d("ISO8583Reply", "override - field[$fieldNo]: $value")
                isoReply.set(fieldNo, value?.toByteArray())
            } else {
                val currentTimestamp = ISO8583Util.getTimeStamp()
                when (fieldNo) {
                    12, 38 -> {
                        isoReply.set(fieldNo, currentTimestamp.toDateString("hhmmss").toByteArray())
                    }

                    13 -> {
                        isoReply.set(fieldNo, currentTimestamp.toDateString("MMdd").toByteArray())
                    }

                    37 -> {
                        isoReply.set(fieldNo, currentTimestamp.toDateString("YYMMddhhmmss").toByteArray())
                    }

                    39 -> {
                        isoReply.set(fieldNo, "00".toByteArray())
                    }
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

    private fun getChannelConfig(host: String, port: String): SimpleConfiguration {
        val config = SimpleConfiguration()
        config.put("host", host)
        config.put("port", port)
        return config
    }

    override fun stopServer() {
        server?.disconnect()
        serverSocket?.close()
        super.stopServer()
    }
}