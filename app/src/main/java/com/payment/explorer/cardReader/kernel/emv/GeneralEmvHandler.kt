package com.payment.explorer.cardReader.kernel.emv

import android.util.Log
import com.payment.explorer.cardReader.kernel.EmvKernel
import com.payment.explorer.extension.hexToBinary
import com.payment.explorer.extension.toHexString
import com.payment.explorer.model.EMVPublicKey
import com.payment.explorer.model.HashMethod
import com.payment.explorer.model.PaymentMethod
import com.payment.explorer.util.APDU_COMMAND_GPO_WITHOUT_PDOL
import com.payment.explorer.util.APDU_RESPONSE_CODE_OK
import com.payment.explorer.util.CVM_NO_CVM_BINARY_CODE
import com.payment.explorer.util.CVM_SIGNATURE_BINARY_CODE
import com.payment.explorer.util.EMVUtils
import com.payment.explorer.util.Encryption
import com.payment.explorer.util.HashUtil
import com.payment.explorer.util.PreferencesUtil
import com.payment.explorer.util.TlvUtil

open class GeneralEmvHandler(val kernel: EmvKernel) : BasicEmvHandler(kernel) {
    override fun onTapProcess() {
        selectAID()
        executeGPO()
        readRecord()
        when (EMVUtils.getPaymentMethodByAID(kernel.core.getICCTag("4F") ?: "")) {
            PaymentMethod.VISA,
            PaymentMethod.DISCOVER,
            PaymentMethod.DINERS,
            PaymentMethod.UNIONPAY -> {
                Log.d("Kernel0", "skip generate AC")
            }

            else -> generateAC()
        }
    }

    override fun postTapProcess() {
        performODA()
        performCVM()
    }

    private fun getHash(plaintext: String): String {
        val hash = HashUtil.hexGetHash(plaintext, HashMethod.SHA1).uppercase()
        Log.d("Kernel0", "plaintext: $plaintext -> hash: $hash")
        return hash
    }

    open fun selectAID() {
        kernel.apply {
            Log.d("Kernel0", "selectAID")
            core.getICCTag("4F")?.also { aid ->
                val cla = "00"
                val ins = "A4"
                val p1 = "04"
                val p2 = "00"
                val aidSize = (aid.length / 2).toHexString()
                Log.d("selectAID", "aidSize: $aidSize")
                val le = "00"
                val apduCommand = "$cla$ins$p1$p2$aidSize$aid$le"

                val apdu = communicator(apduCommand)
                processTlv(apdu.payload)

                core.getICCTag("9F5A")?.let {
                    core.saveICCData(
                        mapOf(
                            "9F42" to it.substring(2, 6),
                            "5F28" to it.substring(6)
                        )
                    )
                }
            } ?: run {
                throw Exception("AID_NOT_FOUND")
            }
        }
    }

    open fun executeGPO() {
        kernel.apply {
            Log.d("Kernel0", "executeGPO")
            val tlv = core.getICCTag("9F38")?.let { pdol ->
                val cla = "80"
                val ins = "A8"
                val p1 = "00"
                val p2 = "00"

                val pdolMap = TlvUtil.readDOL(pdol)
                Log.d("executeGPO", "pdolMap: $pdolMap")
                val sb = StringBuilder()
                pdolMap.forEach {
                    sb.append(core.getICCTag(it.key) ?: core.getTerminalTag(it.key) ?: "00".repeat(it.value.toInt(16)))
                    Log.d("executeGPO", "${it.key}: $sb")
                }

                val data = sb.toString()
                val lc = (data.length / 2 + 2).toHexString()
                Log.d("executeGPO", "lc: $lc")

                val fixByte = "83"

                val dataSizeHex = (data.length / 2).toHexString()
                Log.d("executeGPO", "dataSizeHex: $dataSizeHex")

                val le = "00"

                val apduCommand = "$cla$ins$p1$p2$lc$fixByte$dataSizeHex$data$le"
                communicator(apduCommand).payload
            } ?: run {
                communicator(APDU_COMMAND_GPO_WITHOUT_PDOL).payload
            }

            // handle Format 1 data
            if (tlv.startsWith("80")) {
                val tag80Data = TlvUtil.findByTag(tlv, "80")?.first()
                tag80Data?.let {
                    val aip = it.substring(0, 4) // AIP
                    val afl = it.substring(4)// AFL
                    core.saveICCData(
                        mapOf(
                            "82" to aip,
                            "94" to afl
                        )
                    )
                }
            } else {
                processTlv(tlv)
            }
        }
    }

    open fun readRecord() {
        kernel.apply {
            Log.d("Kernel0", "readRecord")
            val cla = "00"
            val ins = "B2"
            val le = "00"
            core.getICCTag("94")?.also { afl ->
                if (afl.isNotEmpty()) {
                    val locators = afl.chunked(8)
                    Log.d("readRecord", "locators: $locators")

                    locators.forEach {
                        val sfi = it.take(2)
                        Log.d("readRecord", "sfi: $sfi")
                        val p2 = ("${sfi.hexToBinary().take(5)}000".toInt(2) + "0100".toInt(2)).toHexString()
                        val firstRecord = it.substring(2, 4).toInt(16)
                        val lastRecord = it.substring(4, 6).toInt(16)
                        var odaLabel = it.substring(7).toInt(16)
                        Log.d("readRecord", "p2: $p2, firstRecord: $firstRecord, lastRecord: $lastRecord, odaLabel: $odaLabel")
                        for (i in firstRecord..lastRecord) {
                            val p1 = String.format("%02d", i)
                            val cmd = "$cla$ins$p1$p2$le"
                            val tlv = communicator(cmd).payload
                            Log.d("readRecord", "tlv: $tlv")
                            if (tlv.endsWith(APDU_RESPONSE_CODE_OK)) {
                                if (odaLabel > 0) {
                                    core.saveOdaData(TlvUtil.findByTag(tlv, "70")?.first() ?: "")
                                    odaLabel--
                                }
                                processTlv(tlv)
                            }
                        }
                    }
                    Log.d("readRecord", "odaData: ${core.odaData}")
                }
            } ?: run {
                Log.d("readRecord", "AFL_NOT_FOUND")
//            throw Exception("AFL_NOT_FOUND")
            }
        }
    }

    open fun getChallenge(): String? = ""

    open fun generateAC() {
        kernel.apply {
            Log.d("Kernel0", "generateAC")
            val cla = "80"
            val ins = "AE"
            val isTerminalSupportCDA = core.getTerminalTag("9F33")?.takeLast(1)?.hexToBinary()?.get(0) == '1'
            Log.d("generateAC", "isTerminalSupportCDA: $isTerminalSupportCDA")
            val isCardSupportCDA = core.getICCTag("82")?.hexToBinary()?.get(7) == '1'
            // 00:(AAC), 40:(TC) (offline transaction), 80:(ARQC)
            val p1 = if (isTerminalSupportCDA && isCardSupportCDA) ("80".toInt(16) + "10000".toInt(2)).toHexString() else "80"
            val p2 = "00"

            core.getICCTag("8C")?.also { cdol ->
                val cdolMap = TlvUtil.readDOL(cdol)
                Log.d("generateAC", "cdolMap: $cdolMap")
                val sb = StringBuilder()
                cdolMap.forEach {
                    sb.append(core.getICCTag(it.key) ?: core.getTerminalTag(it.key) ?: "00".repeat(it.value.toInt(16)))
                }

                val data = sb.toString()
                val lc = (data.length / 2).toHexString()
                Log.d("generateAC", "lc: $lc")

                val le = "00"

                val apduCommand = "$cla$ins$p1$p2$lc$data$le"
                val tlv = communicator(apduCommand).payload

                // handle Format 1 data
                if (tlv.startsWith("80")) {
                    val tag80Data = TlvUtil.findByTag(tlv, "80")?.first()
                    tag80Data?.let {
                        val cid = it.substring(0, 2)
                        val atc = it.substring(2, 6)
                        val ac = it.substring(6, 22)
                        val iad = it.substring(22)
                        core.saveICCData(
                            mapOf(
                                "9F27" to cid,
                                "9F36" to atc,
                                "9F26" to ac,
                                "9F10" to iad
                            )
                        )

                    }
                } else {
                    processTlv(tlv)
                }
            } ?: run {
                throw Exception("CDOL_NOT_FOUND")
            }
        }
    }

    open fun performODA() {
        kernel.apply {
            Log.d("Kernel0", "ODA")
            var isTerminalSupportSDA = false
            var isTerminalSupportDDA = false
            var isTerminalSupportCDA = false
            core.getTerminalTag("9F33")?.also { termCap ->
                Log.d("performODA", "termCap: $termCap, (${termCap.hexToBinary()})")
                isTerminalSupportSDA = termCap.hexToBinary()[16] == '1'
                isTerminalSupportDDA = termCap.hexToBinary()[17] == '1'
                isTerminalSupportCDA = termCap.hexToBinary()[20] == '1'
            }
            Log.d("performODA", "Terminal Capabilities:\n SDA: $isTerminalSupportSDA \n DDA: $isTerminalSupportDDA \n CDA: $isTerminalSupportCDA")

            core.getICCTag("82")?.also { aip ->
                Log.d("performODA", "aip: $aip")
                val isCardSupportSDA = aip.hexToBinary()[1] == '1'
                val isCardSupportDDA = aip.hexToBinary()[2] == '1'
                val isCardSupportCDA = aip.hexToBinary()[7] == '1'
                Log.d("performODA", "Application Interchange Profile:\n SDA: $isCardSupportSDA \n DDA: $isCardSupportDDA \n CDA: $isCardSupportCDA")
                when {
                    isCardSupportCDA && isTerminalSupportCDA -> dynamicDataAuthentication(true).also { result ->
                        if (result) Log.d("performODA", "CDA success") else Log.d("performODA", "CDA Fail")
                    }

                    isCardSupportDDA && isTerminalSupportDDA -> dynamicDataAuthentication().also { result ->
                        if (result) Log.d("performODA", "DDA success") else Log.d("performODA", "DDA Fail")
                    }

                    isCardSupportSDA && isTerminalSupportSDA -> staticDataAuthentication().also { result ->
                        if (result) Log.d("performODA", "SDA success") else Log.d("performODA", "SDA Fail")
                    }

                    else -> {
                        Log.d("performODA", "ODA not performed")
                        // TODO: set TVR: ODA not performed
                    }
                }
            } ?: throw Exception("AIP_NOT_FOUND")
        }
    }

    private fun dynamicDataAuthentication(isCDA: Boolean = false): Boolean {
        kernel.apply {
            val issuerPK = retrieveIssuerPK() ?: run {
                Log.d("_DDA/_CDA", "Retrieve Issuer Public Key Fail")
                return false
            }
            Log.d("_DDA/_CDA", "issuerPK: ${issuerPK.exponent}, ${issuerPK.modulus}")

            val iccPK = retrieveIccPK(issuerPK) ?: run {
                Log.d("_DDA/_CDA", "Retrieve ICC Public Key Fail")
                return false
            }
            Log.d("_DDA/_CDA", "iccPK: ${iccPK.exponent}, ${iccPK.modulus}")

            val sdad = core.getICCTag("9F4B") // returned from GPO
            val decryptedSDAD = sdad?.let {
                // fDDA
                iccPK.exponent ?: return false
                iccPK.modulus ?: return false
                Encryption.doRSA(it, iccPK.exponent, iccPK.modulus)
            } ?: return false
            Log.d("_DDA/_CDA", "decryptedSDAD: $decryptedSDAD")
            if (!verifySDAD(decryptedSDAD)) return false
            Log.d("_DDA/_CDA", "verifySDAD SUCCESS")

            if (isCDA) saveIccDynamicData(decryptedSDAD) // retrieve and save Application Cryptogram [9F26]

            Log.d("_DDA/_CDA", "Dynamic Data Authentication SUCCESS, CDA: $isCDA")
            return true
        }
    }

    private fun verifySDAD(cert: String): Boolean {
        kernel.apply {
            if (!Regex("^6A(05|95).*", setOf(RegexOption.IGNORE_CASE)).matches(cert)) return false
            if (!cert.endsWith("BC", ignoreCase = true)) return false
            val hash = cert.substring(cert.length - 42, cert.length - 2)
            Log.d("verifySDAD", "hash: $hash")
            // refer to EMV Contactless Book C-3: Table C-1
            return core.getICCTag("9F69")?.let { cardAuthData -> // indicates fDDA perform
                val sb = StringBuilder()
                sb.append(core.getTerminalTag("9F37"))
                sb.append(core.getTerminalTag("9F02"))
                sb.append(core.getTerminalTag("5F2A"))
                sb.append(cardAuthData)
                val terminalDynamicData = sb.toString()
                Log.d("verifySDAD", "terminalDynamicData: $terminalDynamicData")
                val inputData = "${cert.substring(2, cert.length - 42)}$terminalDynamicData"
                getHash(inputData) == hash
            } ?: run {// CDA
                val inputData = "${cert.substring(2, cert.length - 42)}${core.getTerminalTag("9F37")}"
                getHash(inputData) == hash
            }
        }
    }

    private fun saveIccDynamicData(decryptedSDAD: String) {
        kernel.apply {
            val length = decryptedSDAD.substring(6, 8).toInt(16) * 2
            val iccDynamicData = decryptedSDAD.substring(8, 8 + length)
            val iccDynamicNumberLength = iccDynamicData.substring(0, 2).toInt(16) * 2
            val iccDynamicNumber = iccDynamicData.substring(2, 2 + iccDynamicNumberLength)
            Log.d("saveIccDynamicData", "iccDynamicNumber: $iccDynamicNumber")
            val cid = iccDynamicData.substring(2 + iccDynamicNumberLength, 4 + iccDynamicNumberLength)
            Log.d("saveIccDynamicData", "cid: $cid")
            val ac = iccDynamicData.substring(4 + iccDynamicNumberLength, 20 + iccDynamicNumberLength)
            val txnDataHashCode = iccDynamicData.substring(iccDynamicData.length - 40, iccDynamicData.length)
            Log.d("saveIccDynamicData", "txnDataHashCode: $txnDataHashCode")
            // save cryptogram to icc data
            core.saveICCData(
                mapOf(
                    "9F26" to ac,
                )
            )
        }
    }

    private fun retrieveIssuerPK(): EMVPublicKey? {
        kernel.apply {
            val capkIdx = core.getICCTag("8F")
            val capkList = PreferencesUtil.getCapkData(context)
            val capk = capkList.data?.find { it.index == capkIdx } ?: return null
            Log.d("getIssuerPK", "capk: $capk")
            val issuerPKCert = core.getICCTag("90") ?: return null
            Log.d("getIssuerPK", "issuerPKCert: $issuerPKCert")
            val decryptedIssuerPKCert = Encryption.doRSA(issuerPKCert, capk.exponent, capk.modulus)
            Log.d("getIssuerPK", "decryptedIssuerPKCert: $decryptedIssuerPKCert")
            if (!verifyIssuerPKCert(decryptedIssuerPKCert)) return null
            Log.d("getIssuerPK", "verifyIssuerPKCert SUCCESS")

            val length = decryptedIssuerPKCert.substring(26, 28).toInt(16) * 2
            val issuerPKRemainder = core.getICCTag("92") ?: ""
            val issuerPKModulus = "${decryptedIssuerPKCert.substring(30, 30 + length - issuerPKRemainder.length)}${issuerPKRemainder}"
            Log.d("getIssuerPK", "modulus: $issuerPKModulus")
            val issuerPKExponent = core.getICCTag("9F32") ?: return null
            return if (issuerPKModulus.length == length) EMVPublicKey(issuerPKExponent, issuerPKModulus) else null
        }
    }

    private fun verifyIssuerPKCert(cert: String): Boolean {
        kernel.apply {
            if (!cert.startsWith("6A02", ignoreCase = true)) return false
            if (!cert.endsWith("BC", ignoreCase = true)) return false
            val hash = cert.substring(cert.length - 42, cert.length - 2)
            Log.d("verifyIssuerPKCert", "hash: $hash")
            val issuerPKRemainder = core.getICCTag("92") ?: ""
            val issuerPKExponent = core.getICCTag("9F32") ?: ""
            val inputData = "${cert.substring(2, cert.length - 42)}${issuerPKRemainder}${issuerPKExponent}"
            return getHash(inputData) == hash
        }
    }

    private fun retrieveIccPK(issuerPK: EMVPublicKey): EMVPublicKey? {
        kernel.apply {
            val iccPKCert = core.getICCTag("9F46") ?: return null
            issuerPK.exponent ?: return null
            issuerPK.modulus ?: return null
            val decryptedIccPKCert = Encryption.doRSA(iccPKCert, issuerPK.exponent, issuerPK.modulus)
            Log.d("getIccPK", "decryptedIccPKCert: $decryptedIccPKCert")
            if (!verifyIccPKCert(decryptedIccPKCert)) return null
            Log.d("getIccPK", "verifyIccPKCert SUCCESS")

            val length = decryptedIccPKCert.substring(38, 40).toInt(16) * 2
            val iccPKRemainder = core.getICCTag("9F48") ?: ""
            val iccPKModulus = "${decryptedIccPKCert.substring(42, 42 + length - iccPKRemainder.length)}${iccPKRemainder}"
            Log.d("getIccPK", "modulus: $iccPKModulus")
            val iccPKExponent = core.getICCTag("9F47") ?: return null
            return if (iccPKModulus.length == length) EMVPublicKey(iccPKExponent, iccPKModulus) else null
        }
    }

    private fun verifyIccPKCert(cert: String): Boolean {
        kernel.apply {
            if (!cert.startsWith("6A04", ignoreCase = true)) return false
            if (!cert.endsWith("BC", ignoreCase = true)) return false
            val hash = cert.substring(cert.length - 42, cert.length - 2)
            Log.d("verifyIccPKCert", "hash: $hash")
            val iccPKRemainder = core.getICCTag("9F48") ?: ""
            val iccPKExponent = core.getICCTag("9F47") ?: ""
            val staticAuthData = getStaticAuthData()
            Log.d("verifyIccPKCert", "staticAuthData: $staticAuthData")
            val inputData = "${cert.substring(2, cert.length - 42)}${iccPKRemainder}${iccPKExponent}$staticAuthData"
            return getHash(inputData) == hash
        }
    }

    private fun staticDataAuthentication(): Boolean {
        kernel.apply {
            val issuerPK = retrieveIssuerPK() ?: run {
                Log.d("_SDA", "Retrieve Issuer Public Key Fail")
                return false
            }
            Log.d("_SDA", "issuerPK: ${issuerPK.exponent}, ${issuerPK.modulus}")
            val ssad = core.getICCTag("93") ?: return false
            issuerPK.exponent ?: return false
            issuerPK.modulus ?: return false
            val decryptedSSAD = Encryption.doRSA(ssad, issuerPK.exponent, issuerPK.modulus)
            Log.d("_SDA", "decryptedSSAD: $decryptedSSAD")
            if (!verifySSAD(decryptedSSAD)) return false
            Log.d("_SDA", "verifySDAD SUCCESS")
            return true
        }
    }

    private fun verifySSAD(cert: String): Boolean {
        if (!cert.startsWith("6A03", ignoreCase = true)) return false
        if (!cert.endsWith("BC", ignoreCase = true)) return false
        val hash = cert.substring(cert.length - 42, cert.length - 2)
        Log.d("verifySSAD", "hash: $hash")
        val staticAuthData = getStaticAuthData()
        Log.d("verifySSAD", "staticAuthData: $staticAuthData")
        val inputData = "${cert.substring(2, cert.length - 42)}$staticAuthData"
        return getHash(inputData) == hash
    }

    private fun getStaticAuthData(): String {
        kernel.apply {
            var data = ""
            core.getICCTag("9F4A")?.let { tagList ->
                TlvUtil.readTagList(tagList).forEach { tag ->
                    data += core.getICCTag(tag) ?: core.getTerminalTag(tag) ?: ""
                }
            }
            return "${core.odaData}$data"
        }
    }

    open fun performCVM() {
        kernel.apply {
            Log.d("Kernel0", "CVM")
            // Assume terminal only support signature
            core.getICCTag("8E")?.let { cvmList ->
                cvmKernel2(cvmList)
            } ?: run {
                cvmKernel3()
            }
        }
    }

    private fun cvmKernel2(cvmList: String) {
        kernel.apply {
            //Kernel 2 (Mastercard)
            Log.d("performCVM", "cvmList: $cvmList")
            val amountX = cvmList.substring(0, 8).toInt(16)
            val amountY = cvmList.substring(8, 16).toInt(16)
            val cvRules = cvmList.substring(16).chunked(4)
            Log.d("performCVM", "amountX: $amountX, amountY: $amountY, cvRules: $cvRules")

            val isTerminalSupportCDCVM = core.getTerminalTag("9F66")?.let { ttq ->
                ttq.hexToBinary()[17] == '1'
            } ?: false

            val isCardSupportCDCVM = core.getICCTag("82")?.let { aip ->
                aip.hexToBinary()[6] == '1'
            } ?: false

            val exceededCVMRequiredLimit = core.getTerminalTag("DF8126")?.toInt()?.let { cvmRequiredLimit ->
                core.getTerminalTag("9F02")?.toInt()?.let { it >= cvmRequiredLimit }
                    ?: throw Exception("INVALID_AUTHORISED_AMOUNT")
            } ?: false

            if (isTerminalSupportCDCVM && isCardSupportCDCVM) {
                if (exceededCVMRequiredLimit) {
                    // CDCVM success -> CVM result = 010002
                    Log.d("performCVM", "Card Verification Method: CDCVM - SUCCESS")
                    core.saveTerminalData(mapOf("9F34" to "010002"))
                } else {
                    Log.d("performCVM", "Card Verification Method: No CVM - SUCCESS")
                    core.saveTerminalData(mapOf("9F34" to "3F0002"))
                }
            } else {
                cvRules.forEach { rule ->
                    val cvmCode = rule.take(2)
                    val conditionCode = rule.takeLast(2)
//                  Log.d("performCVM", "conditionCode: $conditionCode")
                    val haveSucceeding = cvmCode.hexToBinary()[1] == '1'
                    val txnAmount = core.getTerminalTag("9F02")?.toInt() ?: throw Exception("INVALID_AUTHORISED_AMOUNT")
                    val txnCardEqualCurrency = core.getTerminalTag("5F2A") == core.getTerminalTag("9F42")
                    val matchCondition = when (conditionCode) {
                        "00" -> true
                        "03" -> {
                            when {
                                cvmCode.hexToBinary().takeLast(6) == CVM_SIGNATURE_BINARY_CODE -> true
                                cvmCode.hexToBinary().takeLast(6) == CVM_NO_CVM_BINARY_CODE -> true
                                cvmCode.hexToBinary().takeLast(6).toInt(2) in 1..5 -> false // TODO: Support pin
                                else -> false
                            }
                        }

                        "05" -> core.getTerminalTag("9F03") != "000000000000"
                        "06" -> txnCardEqualCurrency && txnAmount < amountX
                        "07" -> txnCardEqualCurrency && txnAmount >= amountX
                        "08" -> txnCardEqualCurrency && txnAmount < amountY
                        "09" -> txnCardEqualCurrency && txnAmount >= amountY

                        else -> false
                    }
                    Log.d("performCVM", "Current CV rule: $cvmCode, match condition: $matchCondition")

                    if (!matchCondition) {
                        return@forEach
                    }

                    when {
                        cvmCode.hexToBinary().takeLast(6) == CVM_SIGNATURE_BINARY_CODE -> {
                            Log.d("performCVM", "Card Verification Method: Signature - SUCCESS")
                            core.saveTerminalData(mapOf("9F34" to "$cvmCode${conditionCode}02"))
                            return
                        }

                        cvmCode.hexToBinary().takeLast(6) == CVM_NO_CVM_BINARY_CODE -> {
                            Log.d("performCVM", "Card Verification Method: No CVM - SUCCESS")
                            core.saveTerminalData(mapOf("9F34" to "$cvmCode${conditionCode}02"))
                            return
                        }

                        cvmCode.hexToBinary().takeLast(6).toInt(2) in 1..5 -> {
                            Log.d("performCVM", "PIN not support, apply succeeding: $haveSucceeding")
                            //TODO: Support pin
                            if (!haveSucceeding) {
                                core.saveTerminalData(mapOf("9F34" to "$cvmCode${conditionCode}02"))
                                return
                            }
                        }

                        else -> {
                            if (!haveSucceeding) {
                                Log.d("performCVM", "undefined CVM - FAIL")
                                core.saveTerminalData(mapOf("9F34" to "$cvmCode${conditionCode}01"))
                                return
                            }
                        }
                    }
                }
            }
        }
    }

    private fun cvmKernel3() {
        //Kernel 3 (Visa)
        kernel.apply {
            var isTerminalSupportOnlinePin = false
            var isTerminalSupportSignature = false
            var isTerminalSupportCDCVM = false
            core.getTerminalTag("9F66")?.also { ttq ->
                Log.d("performCVM", "ttq: $ttq, (${ttq.hexToBinary()})")
                isTerminalSupportOnlinePin = ttq.hexToBinary()[5] == '1'
                isTerminalSupportSignature = ttq.hexToBinary()[6] == '1'
                isTerminalSupportCDCVM = ttq.hexToBinary()[17] == '1'
            }
            Log.d("performCVM", "Terminal Transaction Qualifiers:\n Online Pin: $isTerminalSupportOnlinePin \n Signature: $isTerminalSupportSignature \n CDCVM: $isTerminalSupportCDCVM")

            core.getICCTag("9F6C")?.also { ctq ->
                Log.d("performCVM", "ctq: $ctq, (${ctq.hexToBinary()})")
                val isCardRequireOnlinePin = ctq.hexToBinary()[0] == '1'
                val isCardRequireSignature = ctq.hexToBinary()[1] == '1'
                val isCardPerformedCDCVM = ctq.hexToBinary()[8] == '1'
                Log.d("performCVM", "Card Transaction Qualifiers:\n Online Pin: $isCardRequireOnlinePin \n Signature: $isCardRequireSignature \n CDCVM: $isCardPerformedCDCVM")

                when {
                    isCardRequireOnlinePin && isTerminalSupportOnlinePin -> {
                        // TODO: Support online pin
                    }

                    isCardPerformedCDCVM && isTerminalSupportCDCVM -> {
                        core.getICCTag("9F69")?.also {
                            if (it.takeLast(4) == ctq) {
                                Log.d("performCVM", "Card Verification Method: CDCVM - SUCCESS")
                            } else {
                                Log.d("performCVM", "Card Verification Method: CDCVM - FAIL")
                            }
                        } ?: run {
                            //check application cryptogram is ARQC
                            if (core.getICCTag("9F27") == "80") {
                                Log.d("performCVM", "Card Verification Method: CDCVM - SUCCESS")
                            } else {
                                Log.d("performCVM", "Card Verification Method: CDCVM - FAIL")
                            }
                        }
                    }

                    isCardRequireSignature && isTerminalSupportSignature -> {
                        Log.d("performCVM", "Card Verification Method: Signature - SUCCESS")
                    }

                    else -> {
                        Log.d("performCVM", "Card Verification Method: No CVM - FAIL")
                    }
                }
            } ?: run {
                Log.d("performCVM", "no ctq returned")
                when {
                    isTerminalSupportSignature -> {
                        Log.d("performCVM", "Card Verification Method: Signature - SUCCESS")
                    }

                    isTerminalSupportCDCVM && isTerminalSupportOnlinePin -> { // CDCVM is mandatory
                        // TODO: Support online pin
                    }

                    else -> {
                        Log.d("performCVM", "Card Verification Method: No CVM - FAIL")
                    }
                }
            }
        }
    }
}