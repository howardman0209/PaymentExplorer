package com.payment.explorer.cardReader.android

import android.app.Activity
import android.nfc.NfcAdapter
import com.payment.explorer.cardReader.BasicCardReader
import com.payment.explorer.cardReader.CardReaderCallback
import com.payment.explorer.cardReader.android.nfc.EmvNfcCallback
import com.payment.explorer.cardReader.emv.EmvKernel
import com.payment.explorer.cardReader.emv.EmvKernelCallback
import com.payment.explorer.cardReader.emv.EmvKernelProvider
import com.payment.explorer.cardReader.model.APDU
import com.payment.explorer.cardReader.model.CardReaderStatus

class AndroidCardReader(private val activity: Activity, handler: CardReaderCallback) : BasicCardReader(activity.applicationContext, handler) {
    private var nfcAdapter: NfcAdapter? = null
    private var emvKernel: EmvKernelProvider? = null
    override fun prepareEmvKernel() {
        emvKernel = EmvKernel(
            object : EmvKernelCallback {
                override fun onApduExchange(apdu: APDU) {
                    handler.onApduExchange(apdu)
                }

                override fun onEmvStarted() {
                    handler.updateReaderStatus(CardReaderStatus.PROCESSING)
                }

                override fun onTapEmvCompleted() {
                    handler.updateReaderStatus(CardReaderStatus.CARD_READ_OK)
                    emvKernel?.postTapEmvProcess()
                }

                override fun postTapEmvCompleted(iccData: Map<String, String>) {
                    handler.updateReaderStatus(CardReaderStatus.SUCCESS)
                    handler.onTransactionOnline(constructOnlineTlv(iccData))
                }

                override fun onError(exception: Exception) {
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
}