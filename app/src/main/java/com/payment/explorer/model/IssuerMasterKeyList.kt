package com.payment.explorer.model

data class IssuerMasterKeyList(
    val data: HashMap<PaymentMethod, String>? = null
)