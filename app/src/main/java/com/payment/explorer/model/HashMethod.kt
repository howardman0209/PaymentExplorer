package com.payment.explorer.model

enum class HashMethod(val algorithm: String) {
    MD5("MD5"),
    SHA1("SHA"),
    SHA224("SHA-224"),
    SHA256("SHA-256"),
}