package com.payment.explorer.util

import com.payment.explorer.extension.hexToBinary

object ISO8583Util {
    fun getFieldsFromHex(isoMsg: String): List<Int> {
        return isoMsg.hexToBinary().mapIndexedNotNull { index, bit ->
            bit.takeIf { it == '1' }?.let { index + 1 }
        }
    }

    fun getTimeStamp() = System.currentTimeMillis()
}