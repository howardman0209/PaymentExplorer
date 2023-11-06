package com.mobile.gateway.util

import com.mobile.gateway.extension.hexToBinary

object ISO8583Util {
    fun getFieldsFromHex(isoMsg: String): List<Int> {
        return isoMsg.hexToBinary().mapIndexedNotNull { index, bit ->
            bit.takeIf { it == '1' }?.let { index + 1 }
        }
    }

    fun getTimeStamp() = System.currentTimeMillis()
}