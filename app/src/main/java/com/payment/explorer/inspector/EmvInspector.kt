package com.payment.explorer.inspector

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.payment.explorer.cardReader.model.APDU
import com.payment.explorer.cardReader.model.APDUSource
import com.payment.explorer.extension.findByKey
import com.payment.explorer.extension.hexToBinary
import com.payment.explorer.extension.toDataClass
import com.payment.explorer.extension.toHexString
import com.payment.explorer.util.APDU_COMMAND_2PAY_SYS_DDF01
import com.payment.explorer.util.APDU_RESPONSE_CODE_OK
import com.payment.explorer.util.ODAUtil
import com.payment.explorer.util.TlvUtil

class EmvInspector(val context: Context) : BasicInspector() {
    private val apduList = mutableListOf<APDU>()
    private val transactionData: HashMap<String, String> = hashMapOf()
    private var odaData: String = ""
    private var nextComingODAData: Boolean = false
    private val gsonBeautifier: Gson = GsonBuilder().setPrettyPrinting().create()

    fun clearData() {
        apduList.clear()
        transactionData.clear()
        odaData = ""
    }

    fun startInspect(apdu: APDU) {
        val logBuilder = StringBuilder()
        when (apdu.source) {
            APDUSource.TERMINAL -> {
                when {
                    apdu.payload == APDU_COMMAND_2PAY_SYS_DDF01 -> {
                        logBuilder.append("Select Proximity Payment System Environment (PPSE)")
                        logBuilder.append("\ncAPDU: ")
                        logBuilder.append(apdu.payload)
                    }

                    apdu.payload.startsWith("00A40400") -> {
                        logBuilder.append("Select Application Identifier (AID)")
                        logBuilder.append("\ncAPDU: ")
                        logBuilder.append(apdu.payload)
                        val size = apdu.payload.substringAfter("00A40400").take(2).toInt(16) * 2
                        val aid = apdu.payload.substringAfter("00A40400").substring(2, 2 + size)
                        logBuilder.append("\nAID: $aid")
                    }

                    apdu.payload.startsWith("80A80000") -> {
                        logBuilder.append("Get Processing Options (GPO)")
                        logBuilder.append("\ncAPDU: ")
                        logBuilder.append(apdu.payload)
                        logBuilder.append(processPDOLFromGPO(apdu.payload))
                    }

                    apdu.payload.startsWith("00B2") -> {
                        logBuilder.append("Read Record")
                        logBuilder.append("\ncAPDU: ")
                        logBuilder.append(apdu.payload)
                        val recordNumber = apdu.payload.substringAfter("00B2").substring(0, 2).toInt(16)
                        logBuilder.append("\nRecord number: $recordNumber, ")
                        transactionData["94"]?.also { afl ->
                            val locators = afl.chunked(8)
                            val cla = "00"
                            val ins = "B2"
                            val le = "00"
                            locators.forEach {
                                val sfi = it.take(2)
                                val p2 = ("${sfi.hexToBinary().take(5)}000".toInt(2) + "0100".toInt(2)).toHexString()
                                val firstRecord = it.substring(2, 4).toInt(16)
                                val lastRecord = it.substring(4, 6).toInt(16)
                                var odaLabel = it.substring(7).toInt(16)
                                for (i in firstRecord..lastRecord) {
                                    val p1 = i.toHexString()
                                    val cmd = "$cla$ins$p1$p2$le"
                                    if (cmd == apdu.payload) {
                                        logBuilder.append("Short File Identifier (SFI): $sfi")
                                    }
                                    if (odaLabel > 0) {
                                        if (cmd == apdu.payload) {
                                            logBuilder.append(", ODA data: true")
                                            nextComingODAData = true
                                        }
                                        odaLabel--
                                    }
                                }
                            }
                        }
                    }

                    apdu.payload.startsWith("80AE") -> {
                        logBuilder.append("Generate Application Cryptogram (GenAC)")
                        logBuilder.append("\ncAPDU: ")
                        logBuilder.append(apdu.payload)
                        logBuilder.append(processCDOLFromGenAC(apdu.payload))
                    }

                    apdu.payload == "0084000000" -> {
                        logBuilder.append("Get Challenge")
                        logBuilder.append("\ncAPDU: ")
                        logBuilder.append(apdu.payload)
                    }
                }
            }

            APDUSource.CARD -> {
                logBuilder.append("\nrAPDU: ")
                logBuilder.append("${apdu.payload}\n")
                if (apdu.payload.endsWith(APDU_RESPONSE_CODE_OK)) {
                    try {
                        gsonBeautifier.toJson(TlvUtil.decodeTLV(apdu.payload)).also { jsonString ->
                            logBuilder.append(jsonString)
                            when {
                                apduList.last().payload.startsWith("80A80000") -> {
                                    saveRequiredTransactionData(jsonString, "8294")
                                    if (apdu.payload.startsWith("80")) {
                                        logBuilder.append("\n[82]: ${transactionData["82"]}")
                                        logBuilder.append("\n[94]: ${transactionData["94"]}")
                                    }
                                }

                                apduList.last().payload.startsWith("80AE") -> {
                                    saveRequiredTransactionData(jsonString, "9F109F269F279F369F4B")

                                    transactionData["9F4B"]?.also { sdad ->
                                        try {
                                            val issuerPK = ODAUtil.retrieveIssuerPK(context, transactionData)
                                            val iccPK = ODAUtil.retrieveIccPK(ODAUtil.getStaticAuthData(odaData, transactionData), transactionData, issuerPK)
                                            val cryptogram = ODAUtil.getCryptogramFromSDAD(sdad, iccPK)
                                            Log.d("getInspectLog", "Cryptogram [9F26]: $cryptogram")
                                            logBuilder.append("\n[9F26]: $cryptogram")
                                        } catch (ex: Exception) {
                                            Log.d("getInspectLog", "Exception: $ex")
                                            logBuilder.append("\n[9F26]: Error: ${ex.message}")
                                        }
                                    }

                                    if (apdu.payload.startsWith("80")) {
                                        logBuilder.append("\n[9F10]: ${transactionData["9F10"]}")
                                        logBuilder.append("\n[9F26]: ${transactionData["9F26"]}")
                                        logBuilder.append("\n[9F27]: ${transactionData["9F27"]}")
                                        logBuilder.append("\n[9F36]: ${transactionData["9F36"]}")
                                    }
                                }

                                else -> {
                                    saveRequiredTransactionData(jsonString, "8C8F90929F329F389F469F479F489F4A")

                                    if (nextComingODAData) {
                                        saveODAData(apdu.payload)
                                        nextComingODAData = false
                                    }
                                }
                            }
                        }
                    } catch (ex: Exception) {
                        logBuilder.append("Not in ASN.1")
                    }
                } else {
                    logBuilder.append("Command not supported")
                }
                logBuilder.append("\n")
            }
        }
        log(logBuilder.toString())
        apduList.add(apdu)
    }

    private fun processPDOLFromGPO(cAPDU: String): String {
        val stringBuilder = StringBuilder()
        val data = cAPDU.substring(14).dropLast(2)
        transactionData["9F38"]?.let { TlvUtil.readDOL(it) }?.also { pdolMap ->
            stringBuilder.append("\n*** Processing Options Data [9F38] ***")
            var cursor = 0
            pdolMap.forEach {
                stringBuilder.append("\n[${it.key}]: ${data.substring(cursor, cursor + it.value.toInt(16) * 2)}")
                cursor += it.value.toInt(16) * 2
            }
            stringBuilder.append("\n*** Processing Options Data [9F38] ***")
        }
        Log.d("processPDOLFromGPO", "Processing Options Data [9F38]: $stringBuilder")
        return stringBuilder.toString()
    }

    private fun processCDOLFromGenAC(cAPDU: String): String {
        val stringBuilder = StringBuilder()
        val data = cAPDU.substring(10).dropLast(2)
        transactionData["8C"]?.let { TlvUtil.readDOL(it) }?.also { cdolMap ->
            stringBuilder.append("\n*** Card Risk Management Data [8C] ***")
            var cursor = 0
            cdolMap.forEach {
                stringBuilder.append("\n[${it.key}]: ${data.substring(cursor, cursor + it.value.toInt(16) * 2)}")
                cursor += it.value.toInt(16) * 2
            }
            stringBuilder.append("\n*** Card Risk Management Data [8C] ***")
        }
        Log.d("processCDOLFromGenAC", "Card Risk Management Data: $stringBuilder")
        return stringBuilder.toString()
    }

    private fun saveRequiredTransactionData(jsonString: String, tagList: String) {
        val jsonObject = jsonString.toDataClass<JsonObject>()

        TlvUtil.readTagList(tagList).forEach { tag ->
            if (jsonString.contains(tag, ignoreCase = true)) {
                if (!transactionData.containsKey(tag)) {
                    jsonObject.findByKey(tag).also {
                        if (it.isNotEmpty()) {
                            transactionData[tag] = it.first().asString
                            Log.d("saveRequiredTransactionData", "currentTransactionData: $transactionData")
                            return@forEach
                        }
                    }
                }
            }

            /**
             * Special handling for rAPDU in [Response Message Template Format 1] (tag: 80)
             */
            when (tag) {
                "82", "94" -> {
                    jsonObject.findByKey("80").also {
                        if (it.isNotEmpty()) {
                            val tlv = it.first().asString
                            when (tag) {
                                "82" -> transactionData["82"] = tlv.take(4)
                                "94" -> transactionData["94"] = tlv.substring(4)
                            }
                            Log.d("saveRequiredTransactionData", "currentTransactionData: $transactionData")
                        }
                    }
                }

                "9F10", "9F26", "9F27", "9F36" -> {
                    jsonObject.findByKey("80").also {
                        if (it.isNotEmpty()) {
                            val tlv = it.first().asString
                            when (tag) {
                                "9F27" -> transactionData["9F27"] = tlv.take(2)
                                "9F36" -> transactionData["9F36"] = tlv.substring(2, 6)
                                "9F26" -> transactionData["9F26"] = tlv.substring(6, 22)
                                "9F10" -> transactionData["9F10"] = tlv.substring(22)
                            }
                            Log.d("saveRequiredTransactionData", "currentTransactionData: $transactionData")
                        }
                    }
                }
            }
        }
    }

    private fun saveODAData(rAPDU: String) {
        odaData += TlvUtil.findByTag(rAPDU, "70")?.first() ?: ""
        Log.d("saveODAData", "odaData: $odaData")
    }
}