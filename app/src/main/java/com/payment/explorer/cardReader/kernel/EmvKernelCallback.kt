package com.payment.explorer.cardReader.kernel

import com.payment.explorer.cardReader.model.APDU

interface EmvKernelCallback {
    fun onApduExchange(apdu: APDU)
    fun onEmvStarted()
    fun onTapEmvCompleted()
    fun postTapEmvCompleted(iccData:Map<String,String>)
    fun onError(exception: Exception)
}