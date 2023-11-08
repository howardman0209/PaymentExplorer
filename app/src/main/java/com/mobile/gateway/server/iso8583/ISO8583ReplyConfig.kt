package com.mobile.gateway.server.iso8583

data class ISO8583ReplyConfig(
    val fields: List<Int> = listOf(),
    val data: Map<String, String>? = null
)
