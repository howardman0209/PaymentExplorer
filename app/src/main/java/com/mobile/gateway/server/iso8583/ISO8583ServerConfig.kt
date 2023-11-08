package com.mobile.gateway.server.iso8583

data class ISO8583ServerConfig(
    val host: String,
    val port: String,
    val isProxy: Boolean = false,
    val redirectDestination: String? = null
)
