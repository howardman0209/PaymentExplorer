package com.payment.explorer.cardReader.emv

import com.payment.explorer.cardReader.model.APDU

interface EmvKernelProvider {
    fun loadEmvConfig(emvConfig: HashMap<String, String>)
    fun setApduExchangeBridge(bridge: (cAPDU: APDU) -> APDU)
    fun onTapEmvProcess()
    fun postTapEmvProcess()
    fun onError(exception: Exception)
}