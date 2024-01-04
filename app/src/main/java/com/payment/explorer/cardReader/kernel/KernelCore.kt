package com.payment.explorer.cardReader.kernel

import com.payment.explorer.util.TlvUtil

class KernelCore {
    var cardData: HashMap<String, String> = hashMapOf()
    var terminalData: HashMap<String, String> = hashMapOf()
    var odaData = ""

    fun saveOdaData(data: String) {
        odaData += data
    }

    fun clearOdaData() {
        odaData = ""
    }

    fun saveTerminalData(data: Map<String, String>) {
        terminalData += data
    }

    fun getTerminalTag(tag: String): String? {
        return terminalData[tag]
    }

    fun clearTerminalData() {
        terminalData.clear()
    }

    fun saveICCData(data: Map<String, String>) {
        data.forEach {
            if (!TlvUtil.isTemplateTag(it.key) && !cardData.containsKey(it.key)) {
                cardData[it.key] = it.value
            }
        }
    }

    fun getICCTag(tag: String): String? {
        return cardData[tag]
    }

    fun clearICCData() {
        cardData.clear()
    }
}