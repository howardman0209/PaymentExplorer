package com.payment.explorer.cardReader.android

import android.app.Activity
import android.nfc.NfcAdapter
import android.util.Log
import com.payment.explorer.cardReader.BasicCardReader
import com.payment.explorer.cardReader.CardReaderCallback
import com.payment.explorer.cardReader.android.nfc.EmvNfcCallback
import com.payment.explorer.cardReader.kernel.EmvKernel
import com.payment.explorer.cardReader.kernel.EmvKernelCallback
import com.payment.explorer.cardReader.kernel.EmvKernelProvider
import com.payment.explorer.cardReader.model.APDU
import com.payment.explorer.cardReader.model.CardReaderStatus
import com.payment.explorer.util.DATE_TIME_PATTERN_EMV_9A
import com.payment.explorer.util.DATE_TIME_PATTERN_EMV_9F21
import com.payment.explorer.util.PreferencesUtil
import com.payment.explorer.util.UUidUtil
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

class AndroidCardReader(private val activity: Activity, handler: CardReaderCallback) : BasicCardReader(activity.applicationContext, handler) {
    private var nfcAdapter: NfcAdapter? = null
    private var emvKernel: EmvKernelProvider? = null

    init {
        prepareNfcDetector()
        prepareEmvKernel()
    }

    override fun prepareEmvKernel() {
        emvKernel = EmvKernel(
            activity.applicationContext,
            object : EmvKernelCallback {
                override fun onApduExchange(apdu: APDU) {
                    handler.onApduExchange(apdu)
                }

                override fun onEmvStarted() {
                    handler.updateReaderStatus(CardReaderStatus.PROCESSING)
                }

                override fun onTapEmvCompleted() {
                    disableReader()
                    handler.updateReaderStatus(CardReaderStatus.CARD_READ_OK)
                    emvKernel?.postTapEmvProcess()
                }

                override fun postTapEmvCompleted(iccData: Map<String, String>) {
                    handler.updateReaderStatus(CardReaderStatus.SUCCESS)
                    handler.onTransactionOnline(constructOnlineTlv(iccData))
                }

                override fun onError(exception: Exception) {
                    disableReader()
                    handler.updateReaderStatus(CardReaderStatus.FAIL)
                }

            }
        )
    }

    fun constructOnlineTlv(iccData: Map<String, String>): String {
        val tlv = StringBuilder()
        return tlv.toString()
    }

    override fun prepareNfcDetector() {
        nfcAdapter = NfcAdapter.getDefaultAdapter(context)
    }

    override fun initTransaction(authorizedAmount: String?, cashbackAmount: String?) {
        val terminalConfig = PreferencesUtil.getEmvConfig(activity.applicationContext).data
        val terminalData = mutableMapOf<String, String>()
        terminalData += terminalConfig
        terminalData["9F02"] = authorizedAmount?.padStart(12, '0') ?: throw Exception("INVALID_AUTHORISED_AMOUNT")
        terminalData["9F03"] = cashbackAmount?.padStart(12, '0') ?: "0".padStart(12, '0')
        terminalData["9A"] = getCurrentTime(DATE_TIME_PATTERN_EMV_9A)
        terminalData["9F21"] = getCurrentTime(DATE_TIME_PATTERN_EMV_9F21)
        terminalData["9F37"] = UUidUtil.genHexIdByLength(8).uppercase()
        Log.d("AndroidCardReader", "initTransaction - $terminalData")
        emvKernel?.loadTerminalData(terminalData)
    }

    override fun enableReader() {
        nfcAdapter?.enableReaderMode(
            activity, EmvNfcCallback(emvKernel),
            NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
            null
        )
        handler.updateReaderStatus(CardReaderStatus.READY)
    }

    override fun disableReader() {
        if (!activity.isDestroyed) {
            nfcAdapter?.disableReaderMode(activity)
        }
    }

    private fun getCurrentTime(format: String): String {
        val date = Date(System.currentTimeMillis())
        return SimpleDateFormat(format, Locale.ENGLISH).format(date)
    }
}