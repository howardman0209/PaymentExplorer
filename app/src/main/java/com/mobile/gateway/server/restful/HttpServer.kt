package com.mobile.gateway.server.restful

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
import java.util.concurrent.TimeUnit

class HttpServer(private val port: Int) : BasicServer {
    private var server: NettyApplicationEngine? = null
    override fun getInstance(): NettyApplicationEngine? {
        return server
    }

    override fun startServer(wait: Boolean) {
        server = embeddedServer(Netty, port) {
            install(ContentNegotiation) {
                gson {

                }
            }

            routing {
                post("/message") {
                    val requestData = call.receive<PostMessageRequest>()
                    DebugPanelManager.log("incoming request: /message - request: $requestData")
                    call.respond(mapOf("result" to "success"))
                }
            }
        }.apply {
            start(wait)
        }
    }

    override fun stopServer() {
        server?.stop(0, 0, TimeUnit.SECONDS)
        server = null
    }


}