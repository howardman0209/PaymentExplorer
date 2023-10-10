package com.hello.world.extension

fun ByteArray.toHexString() = joinToString("") { String.format("%02x", it) }
