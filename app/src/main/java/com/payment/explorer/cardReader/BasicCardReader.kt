package com.payment.explorer.cardReader

import android.content.Context

abstract class BasicCardReader(val context: Context, val handler: CardReaderCallback) {
    abstract fun prepareEmvKernel()
    abstract fun prepareNfcDetector()
}