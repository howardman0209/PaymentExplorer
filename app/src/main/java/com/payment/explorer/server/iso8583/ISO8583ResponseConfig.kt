package com.payment.explorer.server.iso8583

data class ISO8583ResponseConfig(
    val profileName: String? = null,
    val filters: Map<String, String>? = null,
    val fields: List<Int> = listOf(),
    val data: Map<String, String>? = null
)
