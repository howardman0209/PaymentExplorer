package com.golden.template.extension

fun Int?.orZero(): Int = this ?: 0

fun Int.toHexString(byte: Int = 2): String {
    val hex = this.toString(16).uppercase()
    return ("${"0".repeat(byte)}$hex").substring(hex.length)
}