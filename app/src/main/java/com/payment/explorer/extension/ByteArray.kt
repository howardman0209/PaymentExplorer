package com.payment.explorer.extension

fun ByteArray.toHexString() = joinToString("") { String.format("%02x", it) }
