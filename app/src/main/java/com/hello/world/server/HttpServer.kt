package com.hello.world.server

import com.hello.world.util.DebugPanelManager
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
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
                    DebugPanelManager.log("incoming request: /message")
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