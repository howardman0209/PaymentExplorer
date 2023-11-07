package com.mobile.gateway.server.restful

import android.util.Log
import com.mobile.gateway.model.PostMessageRequest
import com.mobile.gateway.server.BasicServer
import com.mobile.gateway.util.DebugPanelManager
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.SocketException
import java.util.concurrent.TimeUnit

class HttpServer(private val ip: String, private val port: Int) : BasicServer<NettyApplicationEngine>() {
    override fun startServer(wait: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                embeddedServer(Netty, port) {
                    install(ContentNegotiation) {
                        gson {

                        }
                    }

                    routing {
                        post("/message") {
                            val requestData = call.receive<PostMessageRequest>()
                            DebugPanelManager.log("[RESTFUL] Server - incoming request: /message \n- request: $requestData")
                            call.respond(mapOf("result" to "success"))
                        }
                    }
                }.apply {
                    DebugPanelManager.log("[RESTFUL] Server IP: $ip Port: $port")
                    DebugPanelManager.log("-".repeat(50))
                    server = this
                    start(wait)
                }
            } catch (socketException: SocketException) {
                DebugPanelManager.log("[RESTFUL] Server - socketException: ${socketException.message}")
            } catch (ex: Exception) {
                DebugPanelManager.log("[RESTFUL] Server - Exception: $ex")
            }
        }
    }

    override fun stopServer() {
        server?.stop(0, 0, TimeUnit.SECONDS)
        DebugPanelManager.log("-".repeat(50))
        DebugPanelManager.log("[RESTFUL] Server - Socket closed")
        super.stopServer()
    }
}