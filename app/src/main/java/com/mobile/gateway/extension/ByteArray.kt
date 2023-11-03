package com.mobile.gateway.extension

fun ByteArray.toHexString() = joinToString("") { String.format("%02x", it) }
