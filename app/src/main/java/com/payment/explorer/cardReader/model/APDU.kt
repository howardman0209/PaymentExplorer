package com.payment.explorer.cardReader.model

data class APDU(
    val payload: String,
    val source: APDUSource
)
