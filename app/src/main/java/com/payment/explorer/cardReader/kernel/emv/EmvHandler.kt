package com.payment.explorer.cardReader.kernel.emv

interface EmvHandler {
    fun ppse()
    fun onTapProcess()
    fun postTapProcess()
}