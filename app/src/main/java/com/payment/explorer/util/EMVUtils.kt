package com.payment.explorer.util

import android.content.Context
import com.payment.explorer.extension.adjustDESParity
import com.payment.explorer.extension.hexBitwise
import com.payment.explorer.extension.hexToByteArray
import com.payment.explorer.extension.toHexString
import com.payment.explorer.model.BitwiseOperation
import com.payment.explorer.model.CryptogramKey
import com.payment.explorer.model.EntryMode
import com.payment.explorer.model.PaddingMethod
import com.payment.explorer.model.PaymentMethod
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern
import kotlin.collections.HashMap

object EMVUtils {

    private val qrMap = EnumMap<PaymentMethod, Pattern>(PaymentMethod::class.java)

    private val cardMap = EnumMap<PaymentMethod, Pattern>(PaymentMethod::class.java)

    private val dynamicRules = EnumMap<PaymentMethod, Pattern>(PaymentMethod::class.java)

    /**
     * Refer to
     * https://github.com/stripe/stripe-android/blob/master/stripe/src/main/java/com/stripe/android/model/CardBrand.kt
     *
     * Some of the brands (e.g. CUP & Discover) now follow the above.
     */

    init {
        cardMap[PaymentMethod.VISA] = Pattern.compile("^4[\\w]{12,18}$")
        cardMap[PaymentMethod.MASTER] = Pattern.compile(
            "^(5[1-5][\\w]{14})|2(2(2[1-9][0-9][0-9]|[3-9][\\w]{3})|[3-6][\\w]{4}|7([0-1][\\w]{3}|20[\\w]{2}))[\\w]{10}$"
        )
        cardMap[PaymentMethod.AMEX] = Pattern.compile("^3([47])[\\w]{13}$")
        cardMap[PaymentMethod.DINERS] = Pattern.compile("^(30[0-5][\\w]{11}|3095[\\w]{10}|3([689])[\\w]{12})$")
        cardMap[PaymentMethod.DISCOVER] = Pattern.compile("^(60|64|65)[0-9]*$")
        cardMap[PaymentMethod.JCB] = Pattern.compile("^35(2[89][\\w]{12}|[3-8][\\w]{13})$")
        //cardMap[PaymentMethod.UNIONPAY] = Pattern.compile("^62\\d+|^9\\d+")
        cardMap[PaymentMethod.UNIONPAY] = Pattern.compile("^(62|81)[0-9]*$") //Include 81 test card
        cardMap[PaymentMethod.VERVE] = Pattern.compile("^(65[\\w]{14,17})|(5061[\\w]{12,15})$")

        //FOR GP QR CODE
        cardMap[PaymentMethod.ALIPAY] = Pattern.compile("^000101[\\d]{10}\$")
        cardMap[PaymentMethod.WECHAT] = Pattern.compile("^000102[\\d]{10}\$")

        qrMap[PaymentMethod.ALIPAY] = Pattern.compile("^(((2[5-9])|(30))\\d{14,22})$")
        qrMap[PaymentMethod.WECHAT] = Pattern.compile("^(((1[0-6])|(30))\\d{16})$")
        qrMap[PaymentMethod.EASYCARD] = Pattern.compile("^(99\\d{18})$") //Start with 99 and len = 20

    }

    fun updatePayMethodByPanRules(paymentMethod: PaymentMethod, pattern: String) {
        dynamicRules[paymentMethod] = Pattern.compile(pattern)
    }

    fun clearDynamicRules() {
        dynamicRules.clear()
    }

    /**
     * Util function to lookup payment method by PAN
     * However, in case of dual brand card this function should always return CUP no matter what PAN
     */
    fun getPaymentMethodByPan(iCardNumber: String?, dualBrandCardUseCUP: Boolean? = null): PaymentMethod {
        //Obviously if use dualBrandCard we should always return CUP
        if (dualBrandCardUseCUP == true)
            return PaymentMethod.UNIONPAY

        //Normal lookup payment method by PAN
        if (iCardNumber != null) {
            /**
             * The check order do matter here
             * It's found that the maestro bin table may overlap with other payment method (e.g. CUP). E.g. IIN:621984
             * As the maestro table is given by customer, we should honor them and make it's priority higher than default payment method table
             */

            //Check dynamic rules first. We always trust AID more than PAN
            for (entry in dynamicRules.entries) {
                if (entry.value.matcher(iCardNumber).matches()) {
                    return entry.key
                }
            }

            //Check default payment method regex
            for (entry in cardMap.entries) {
                if (entry.value.matcher(iCardNumber).matches()) {
                    return entry.key
                }
            }
        }
        return PaymentMethod.UNKNOWN
    }

    /**
     * Check payment method by AID
     */
    fun getPaymentMethodByAID(aid: String): PaymentMethod {
        aid.uppercase(Locale.ENGLISH).also {
            when {
                it.startsWith("A0000000031010") -> return PaymentMethod.VISA
                it.startsWith("A0000000041010") -> return PaymentMethod.MASTER
                it.startsWith("A0000000043060") -> return PaymentMethod.MAESTRO
                it.startsWith("A0000003330101") -> return PaymentMethod.UNIONPAY
                it.startsWith("A00000002501") -> return PaymentMethod.AMEX
                it.startsWith("A0000000651010") -> return PaymentMethod.JCB
                it.startsWith("A0000005150004") -> return PaymentMethod.EPS
                it.startsWith("A0000005150024") -> return PaymentMethod.EPS
                it.startsWith("A00000047400000001") -> return PaymentMethod.EPS
                it.startsWith("A000000013000002") -> return PaymentMethod.PLC
                it.startsWith("A000000152") -> return PaymentMethod.DINERS
                it.startsWith("A0000003241010") -> return PaymentMethod.DISCOVER
            }
        }

        return PaymentMethod.UNKNOWN
    }

    /**
     * Mask the incoming PAN
     *
     * excludeFromStart: How many chars will exclude from start of PAN.
     * excludeFromEnd: How many chars will exclude from end of PAN.
     *
     * e.g. PAN: 123456789
     * excludeFromStart: 2
     * excludeFromEnd: 3
     *
     * Result:
     * 12XXXX789
     *
     */
    fun maskIncomingPan(pan: String?, excludeFromStart: Int = 0, excludeFromEnd: Int = 4, maskChar: Char = '*'): String? {
        return if (pan != null) {
            val endReplaceIndex = pan.length - excludeFromEnd
            val totalReplaceChars = endReplaceIndex - excludeFromStart

            if (totalReplaceChars > 0) {
                pan.replaceRange(excludeFromStart until endReplaceIndex, maskChar.toString().repeat(totalReplaceChars))
            } else {
                pan
            }
        } else
            null
    }

    /**
     * Trim the beginning ";" or "%B"   and ending "?X" Sentinel from input RAW track format
     *
     */
    fun trimSentinelFromISOTrack(trackRaw: String): String {
        return trackRaw.trimStart(';', '%', 'B').split('?')[0]
    }

    /**
     * Add the beginning ";" and ending "?X" Sentinel from input track2 to make it ISO7813 compatible
     */
    fun toISOTrack2FromTrack2(track: String): String {
        var tmp = track

        if (!tmp.startsWith(";"))
            tmp = ";$tmp"

        if (!tmp.contains('?')) {
            tmp = "$tmp?0" //Add a dummy LRC in this case
        }

        return tmp
    }

    fun convertExpDateYYMMToYYMMDD(expYYMM: String?): String? {
        if (expYYMM == null)
            return null
        //First index is dummy. In this case we can directly use month as index without -1
        val daysInMonth = byteArrayOf(0, 31, 28, 31, 30, 31, 31, 30, 31, 30, 31, 30, 31)
        val days = daysInMonth[expYYMM.substring(2).toInt()].toString()
        return expYYMM + days
    }

    /**
     *   extractDataFromTrack2
     *   Return Triple data contained PAN, ExpirationDate and Service Code
     *   @param isTag57Format Indicate the Track 2 data is from MSR or from TLV Tag 57
     *   @param expDateAppendDays Indicate when return exp date we need to append a fake day to make it YYMMDD. The day is end of month
     */
    fun extractDataFromTrack2(track2: String?, isTag57Format: Boolean = false, expDateAppendDays: Boolean): Triple<String?, String?, String?>? {

        return if (track2 != null) {

            val track2Tmp = trimSentinelFromISOTrack(track2.uppercase(Locale.ENGLISH))

            val delimiter = if (isTag57Format) "D" else "="

            val arr = track2Tmp.split(delimiter)
            if (arr.count() > 1) {
                val pan = arr[0]

                //T2 exp date is just YYMM by default
                var expirationDate: String? = if (arr[1].length >= 4) arr[1].substring(0, 4) else null

                //Convert it to YYMMDD while DD is end day of month
                if (expDateAppendDays && expirationDate != null) {
                    expirationDate = convertExpDateYYMMToYYMMDD(expirationDate)
                }

                val serviceCode: String? = if (arr[1].length >= 7) arr[1].substring(4, 7) else null

                return Triple(pan, expirationDate, serviceCode)
            } else {
                null
            }
        } else
            null
    }

    /**
     *   Convert from MSR Track data to ICC EQ Track data
     */
    fun trackDataToEqTrackData(trackData: String?): String? {
        return if (trackData != null) {

            val trackTmp = trimSentinelFromISOTrack(trackData)

            var tmp = trackTmp.replace("=", "D")

            if (tmp.length and 1 == 1) {
                tmp += "F"
            }

            return tmp

        } else
            null
    }

    /**
     *   Convert from ICC EQ T2 (tag 57) to MSR T2
     */
    fun track2EqToMSRTrack2(track2: String?): String? {
        return if (track2 != null) {

            //First trim any padding F
            val track2Tmp = trimTrack2EquivalentFromICC(track2)?.uppercase(Locale.ENGLISH)

            //Convert delimiter D to =

            return track2Tmp?.replace("D", "=")

        } else
            null
    }

    /**
     * This function will use 8583 Host reply time to create time stamp
     * The input date is in MMdd while time is in HHmmss format
     * As lack of year, we have to do a smart guess on which year we are
     */
    fun createDateTimeFromHostReply(serverDateMMdd: String, serverTimeHHmmss: String): Date? {
        val localTime = Date()

        //Note: Server didn't reply year so we use local year to init year first
        val yearLocal = SimpleDateFormat("yyyy", Locale.ENGLISH).format(localTime)
        val monthDateLocal = SimpleDateFormat("MMdd", Locale.ENGLISH).format(localTime)

        //Here we assumpe the time difference between local and server is less than 1 day.
        // If time drift too much there's no way to guess year
        val yearGuess = if (monthDateLocal == "1231" && serverDateMMdd == "0101")
            (yearLocal.toInt() + 1).toString() //Local 12/31 and remote 01/01. In this case server year should be local + 1
        else if (monthDateLocal == "0101" && serverDateMMdd == "1231")
            (yearLocal.toInt() - 1).toString() //Local 12/31 and remote 01/01. In this case server year should be local - 1
        else
            yearLocal //Else case just assume local year is the same year

        return SimpleDateFormat("yyyyMMddHHmmss", Locale.ENGLISH).parse(yearGuess + serverDateMMdd + serverTimeHHmmss)
    }

    @Suppress("unused")
    fun entryModeTryParse(mode: String?): EntryMode {
        return when (mode) {
            "01" -> EntryMode.MANUAL
            "05" -> EntryMode.CHIP
            "07" -> EntryMode.CONTACTLESS
            "80" -> EntryMode.FALLBACK
            "90" -> EntryMode.MAGSTRIPE
            "91" -> EntryMode.CONTACTLESS //CTL MSR
            else -> EntryMode.UNKNOWN
        }
    }

    fun entryModeToString(mode: EntryMode?): String {
        return when (mode) {
            EntryMode.MANUAL -> "01"
            EntryMode.MAGSTRIPE -> "90"
            EntryMode.CHIP -> "05"
            EntryMode.CONTACTLESS -> "07"
            EntryMode.FALLBACK -> "80"
            else -> "00"
        }
    }

    /**
     * Refer to https://en.wikipedia.org/wiki/Magnetic_stripe_card
     * extractServiceCode
     */
    fun isChipCard(serviceCode: String): Boolean {
        return (serviceCode.startsWith("2") || serviceCode.startsWith("6"))
    }

    @Suppress("unused")
    fun isCardNeedPin(serviceCode: String): Boolean {
        return (serviceCode.endsWith("0") ||
                serviceCode.endsWith("5") ||
                serviceCode.endsWith("2") ||
                serviceCode.endsWith("6")
                )
    }

    fun isCardExpired(expiryDateYYMM: String): Boolean {
        /**
         * For parsing with the abbreviated year pattern ("y" or "yy"), SimpleDateFormat must interpret the abbreviated year relative to some century.
         * It does this by adjusting dates to be within 80 years before and 20 years after the time the SimpleDateFormat instance is created.
         */
        val format = SimpleDateFormat("yyMM", Locale.ENGLISH).apply {
            set2DigitYearStart(Calendar.getInstance().apply { set(Calendar.YEAR, 2000) }.time)
        }

        val calendar = Calendar.getInstance().apply {
            time = format.parse(expiryDateYYMM)
            add(Calendar.MONTH, 1)
        }
        return calendar.time.before(Date())
    }

    /**
     * Calculate  incoming pan Last digit of PAN using Luhn algorithm
     */
    fun luhnCalculateLastDigit(cardNo: String): String {
        val removePrefix = cardNo.trimStart('0')

        val digit: String
        /* convert to array of int for simplicity */
        val digits = IntArray(removePrefix.length)
        for (i in removePrefix.indices) {
            digits[i] = Character.getNumericValue(removePrefix[i])
        }

        /* double every other starting from right - jumping from 2 in 2 */
        var i = digits.size - 1
        while (i >= 0) {
            digits[i] += digits[i]

            /* taking the sum of digits grater than 10 - simple trick by substract 9 */
            if (digits[i] >= 10) {
                digits[i] = digits[i] - 9
            }
            i -= 2
        }
        var sum = 0
        for (n in digits.indices) {
            sum += digits[n]
        }
        /* multiply by 9 step */
        sum *= 9

        /* convert to string to be easier to take the last digit */
        digit = sum.toString() + ""
        return digit.substring(digit.length - 1)

    }

    /**
     * Check if incoming pan is a valid PAN
     * The Last digit of PAN is a checksum digit using Luhn algorithm
     */
    fun isPanValid(pan: String): Boolean {

        val length = pan.length
        var index = 0
        var tmpTotal = 0


        if (length < 20) {
            for (s: Char in pan) {
                if (!s.isDigit())
                    return false
                else {
                    val tmp = s.toString().toInt()
                    if (index != length - 1) {
                        tmpTotal += if (index and 1 == length and 1) {
                            val k = tmp * 2
                            //In fact the real logic is add the digits and then mod 10. Here the input is 0~9 so output is 0 ~ 18 and we can make a shortcut by -9
                            //e.g. 9 * 2 = 18, 1+8 == 9 so in fact 18 - 9 also ok
                            if (k >= 10)
                                k - 9
                            else
                                k
                        } else
                            tmp
                    } else {
                        tmpTotal *= 9
                        return s == tmpTotal.toString().last()
                    }
                    index++
                }
            }

            return true
        }
        return false
    }

    /**
     * Trim any starting 0 and trailing f for incoming pan
     */
    private fun trimPanFromICC(pan: String?): String? {
        if (pan == null)
            return null

        var tmpPan: String = pan

        //Remove all leading zero
        while (tmpPan.startsWith("0")) {
            tmpPan = tmpPan.substring(1)
        }

        //Remove all trailing F
        while (tmpPan.endsWith("f", true)) {
            tmpPan = tmpPan.substring(0, tmpPan.length - 1)
        }

        return tmpPan
    }

    /**
     * Trim trailing F from Tag 57 T2EQ
     * Refer to https://www.emvlab.org/emvtags/?number=57
     * Field 57 will pad F to ensure whole byte. One must call this function
     * to convert track57 back to ISO/IEC 7813 format
     */
    @Suppress("could be private")
    fun trimTrack2EquivalentFromICC(t2Data: String?): String? {
        return if (t2Data?.endsWith("f", true) == true)
            t2Data.substring(0, t2Data.length - 1)
        else
            t2Data
    }

    fun getIssuerMasterKeyByPan(context: Context, pan: String): String {
        return getIssuerMasterKeyByPaymentMethod(context, getPaymentMethodByPan(pan))
    }

    fun getIssuerMasterKeyByPaymentMethod(context: Context, paymentMethod: PaymentMethod): String {
        val imkMap = PreferencesUtil.getIMKMap(context)
        return imkMap.data?.get(paymentMethod) ?: ""
    }

    fun deriveICCMasterKey(imk: String, pan: String, psn: String): String {
        try {
            val y = "$pan$psn".takeLast(16)
            val zl = Encryption.doTDESEncryptECB(y, imk)
            val zr = Encryption.doTDESEncryptECB(y.hexBitwise(operation = BitwiseOperation.NOT), imk)
            val iccMK = "$zl$zr"
            return iccMK.hexToByteArray().adjustDESParity().toHexString().uppercase()
        } catch (ex: Exception) {
            throw Exception("DERIVE_ICC_MASTER_KEY_ERROR")
        }
    }

    fun deriveACSessionKey(paymentMethod: PaymentMethod, iccMK: String, atc: String, un: String? = null): String {
        try {
            // TODO: handle AMEX
            val f1 = when (paymentMethod) {
                PaymentMethod.VISA,
                PaymentMethod.DINERS,
                PaymentMethod.DISCOVER -> "${atc}F000".padEnd(16, '0')

                PaymentMethod.MASTER -> "${atc}F000$un".padEnd(16, '0')
                PaymentMethod.UNIONPAY -> atc.padStart(16, '0')
                else -> ""
            }

            val f2 = when (paymentMethod) {
                PaymentMethod.VISA,
                PaymentMethod.DINERS,
                PaymentMethod.DISCOVER -> "${atc}0F00".padEnd(16, '0')

                PaymentMethod.MASTER -> "${atc}0F00$un".padEnd(16, '0')
                PaymentMethod.UNIONPAY -> atc.hexBitwise(operation = BitwiseOperation.NOT).padStart(16, '0')
                else -> ""
            }

            val sk = "${Encryption.doTDESEncryptECB(f1, iccMK)}${Encryption.doTDESEncryptECB(f2, iccMK)}"
            return sk.hexToByteArray().adjustDESParity().toHexString().uppercase()
        } catch (ex: Exception) {
            throw Exception("DERIVE_AC_SESSION_KEY_ERROR")
        }
    }

    fun getCVNByPaymentMethod(paymentMethod: PaymentMethod, iad: String): Int? {
        try {
            return when (paymentMethod) {
                PaymentMethod.MASTER,
                PaymentMethod.DINERS,
                PaymentMethod.DISCOVER -> iad.substring(2, 4).toInt()

                PaymentMethod.UNIONPAY,
                PaymentMethod.JCB,
                PaymentMethod.AMEX -> iad.substring(4, 6).toInt(16)

                PaymentMethod.VISA -> {
                    when (iad.substring(6, 8)) {
                        "03" -> iad.substring(4, 6).toInt(16)
                        "00" -> iad.substring(2, 4).toInt(16)
                        else -> throw Exception("UNKNOWN_IAD_FORMAT")
                    }
                }

                else -> null
            }
        } catch (ex: Exception) {
            throw Exception("INVALID_ICC_DATA [9F10]")
        }
    }

    fun getAcTagListByPaymentMethod(paymentMethod: PaymentMethod, cvn: Int?): String {
        val tagList = when {
            paymentMethod == PaymentMethod.VISA && cvn == 17 -> "9F029F379F369F10"
            (paymentMethod == PaymentMethod.DISCOVER || paymentMethod == PaymentMethod.DINERS)
                    && cvn == 15 -> "9F029F1A9F379F369F10"

            else -> "9F029F039F1A955F2A9A9C9F37829F369F10"
        }
        return tagList
    }

    fun getAcDOLByPaymentMethod(paymentMethod: PaymentMethod, cvn: Int?, data: HashMap<String, String>): String {
        try {
            val dolBuilder = StringBuilder()
            val tagList = TlvUtil.readTagList(getAcTagListByPaymentMethod(paymentMethod, cvn))

            when (paymentMethod) {
                PaymentMethod.VISA -> {
                    when (cvn) {
                        10 -> {
                            tagList.forEach {
                                if (it != "9F10") {
                                    dolBuilder.append(data[it] ?: "")
                                } else {
                                    dolBuilder.append(data[it]?.substring(6, 14) ?: "")
                                }
                            }
                        }

                        17 -> {
                            tagList.forEach {
                                if (it != "9F10") {
                                    dolBuilder.append(data[it] ?: "")
                                } else {
                                    dolBuilder.append(data[it]?.substring(8, 10) ?: "")
                                }
                            }
                        }

                        else -> {
                            tagList.forEach {
                                dolBuilder.append(data[it] ?: "")
                            }
                        }
                    }
                }

                PaymentMethod.MASTER -> {
                    when (cvn) {
                        10 -> {
                            tagList.forEach {
                                if (it != "9F10") {
                                    dolBuilder.append(data[it] ?: "")
                                } else {
                                    dolBuilder.append(data[it]?.substring(4, 16) ?: "")
                                }
                            }
                        }

                        else -> {
                            // TODO: calculate other CVN
                            throw Exception("UNHANDLED CRYPTOGRAM VERSION")
                        }
                    }
                }

                PaymentMethod.UNIONPAY -> {
                    when (cvn) {
                        1 -> {
                            tagList.forEach {
                                if (it != "9F10") {
                                    dolBuilder.append(data[it] ?: "")
                                } else {
                                    dolBuilder.append(data[it]?.substring(6, 14) ?: "")
                                }
                            }
                        }

                        else -> {
                            // TODO: calculate other CVN
                            throw Exception("UNHANDLED CRYPTOGRAM VERSION")
                        }
                    }
                }

                PaymentMethod.JCB -> {
                    when (cvn) {
                        1 -> {
                            tagList.forEach {
                                if (it != "9F10") {
                                    dolBuilder.append(data[it] ?: "")
                                } else {
                                    dolBuilder.append(data[it]?.substring(6) ?: "")
                                }
                            }
                        }

                        else -> {
                            // TODO: calculate other CVN
                            throw Exception("UNHANDLED CRYPTOGRAM VERSION")
                        }
                    }
                }

                PaymentMethod.DINERS, PaymentMethod.DISCOVER -> {
                    when (cvn) {
                        15 -> {
                            tagList.forEach {
                                dolBuilder.append(data[it] ?: "")
                            }
                        }

                        else -> {
                            // TODO: calculate other CVN
                            throw Exception("UNHANDLED CRYPTOGRAM VERSION")
                        }
                    }
                }

                PaymentMethod.AMEX -> {
                    when (cvn) {
                        1 -> {
                            tagList.forEach {
                                if (it != "9F10") {
                                    dolBuilder.append(data[it] ?: "")
                                } else {
                                    dolBuilder.append(data[it]?.substring(6) ?: "")
                                }
                            }
                        }

                        else -> {
                            // TODO: calculate other CVN
                            throw Exception("UNHANDLED CRYPTOGRAM VERSION")
                        }
                    }
                }

                else -> {}
            }

            return dolBuilder.toString().uppercase()
        } catch (ex: Exception) {
            throw Exception("RETRIEVE_AC_DOL_ERROR")
        }
    }

    fun getAcDOLPaddingByPaymentMethod(paymentMethod: PaymentMethod, cvn: Int?): PaddingMethod {
        return when (paymentMethod) {
            PaymentMethod.VISA -> {
                return when (cvn) {
                    10, 17 -> PaddingMethod.ISO9797_M1
                    else -> PaddingMethod.ISO9797_M2
                }
            }

            PaymentMethod.MASTER -> {
                return when (cvn) {
                    10 -> PaddingMethod.ISO9797_M2
                    else -> {
                        // TODO: calculate other CVN
                        throw Exception("UNHANDLED CRYPTOGRAM VERSION")
                    }
                }
            }

            PaymentMethod.UNIONPAY -> {
                return when (cvn) {
                    1 -> PaddingMethod.ISO9797_M2
                    else -> {
                        // TODO: calculate other CVN
                        throw Exception("UNHANDLED CRYPTOGRAM VERSION")
                    }
                }
            }

            PaymentMethod.JCB -> {
                return when (cvn) {
                    1 -> PaddingMethod.ISO9797_M1
                    else -> {
                        // TODO: calculate other CVN
                        throw Exception("UNHANDLED CRYPTOGRAM VERSION")
                    }
                }
            }

            PaymentMethod.DINERS,
            PaymentMethod.DISCOVER -> {
                return when (cvn) {
                    15 -> PaddingMethod.ISO9797_M2
                    else -> {
                        // TODO: calculate other CVN
                        throw Exception("UNHANDLED CRYPTOGRAM VERSION")
                    }
                }
            }

            PaymentMethod.AMEX -> {
                return when (cvn) {
                    1 -> PaddingMethod.ISO9797_M1
                    else -> {
                        // TODO: calculate other CVN
                        throw Exception("UNHANDLED CRYPTOGRAM VERSION")
                    }
                }
            }

            else -> PaddingMethod.ISO9797_M1
        }
    }

    fun getACCalculationKey(paymentMethod: PaymentMethod, cvn: Int?): CryptogramKey {
        return when (paymentMethod) {
            PaymentMethod.VISA -> {
                when (cvn) {
                    10 -> CryptogramKey.ICC_MASTER_KEY
                    17 -> CryptogramKey.ICC_MASTER_KEY
                    else -> CryptogramKey.SESSION_KEY
                }
            }

            PaymentMethod.MASTER -> {
                when (cvn) {
                    10 -> CryptogramKey.SESSION_KEY
                    else -> {
                        // TODO: calculate other CVN
                        throw Exception("UNHANDLED CRYPTOGRAM VERSION")
                    }
                }
            }

            PaymentMethod.UNIONPAY -> {
                when (cvn) {
                    1 -> CryptogramKey.SESSION_KEY
                    else -> {
                        // TODO: calculate other CVN
                        throw Exception("UNHANDLED CRYPTOGRAM VERSION")
                    }
                }
            }

            PaymentMethod.JCB -> {
                when (cvn) {
                    1 -> CryptogramKey.ICC_MASTER_KEY
                    else -> {
                        // TODO: calculate other CVN
                        throw Exception("UNHANDLED CRYPTOGRAM VERSION")
                    }
                }
            }

            PaymentMethod.DINERS,
            PaymentMethod.DISCOVER -> {
                when (cvn) {
                    15 -> CryptogramKey.SESSION_KEY
                    else -> {
                        // TODO: calculate other CVN
                        throw Exception("UNHANDLED CRYPTOGRAM VERSION")
                    }
                }
            }

            PaymentMethod.AMEX -> {
                when (cvn) {
                    1 -> CryptogramKey.ICC_MASTER_KEY
                    else -> {
                        // TODO: calculate other CVN
                        throw Exception("UNHANDLED CRYPTOGRAM VERSION")
                    }
                }
            }

            else -> CryptogramKey.ICC_MASTER_KEY
        }
    }
}