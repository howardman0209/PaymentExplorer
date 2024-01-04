package com.payment.explorer.cardReader.kernel

import com.payment.explorer.cardReader.model.APDU

interface EmvKernelProvider {
    fun loadTerminalData(terminalData: Map<String, String>)
    fun setApduExchangeBridge(bridge: (cAPDU: APDU) -> APDU)
    fun onTapEmvProcess()
    fun postTapEmvProcess()
    fun onError(exception: Exception)
}