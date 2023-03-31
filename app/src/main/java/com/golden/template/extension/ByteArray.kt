package com.golden.template.extension

fun ByteArray.toHexString() = joinToString("") { String.format("%02x", it) }
