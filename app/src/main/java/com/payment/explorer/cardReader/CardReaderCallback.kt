package com.payment.explorer.cardReader

import com.payment.explorer.cardReader.model.APDU
import com.payment.explorer.cardReader.model.CardReaderStatus

interface CardReaderCallback {
    fun updateReaderStatus(status: CardReaderStatus)
    fun onApduExchange(apdu: APDU)
    fun onTransactionOnline(iccData: Map<String, String>)
}