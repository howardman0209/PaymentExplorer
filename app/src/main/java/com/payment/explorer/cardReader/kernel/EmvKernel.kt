package com.payment.explorer.cardReader.kernel

import android.content.Context
import android.util.Log
import com.payment.explorer.cardReader.kernel.emv.BasicEmvHandler
import com.payment.explorer.cardReader.kernel.emv.EmvHandler
import com.payment.explorer.cardReader.kernel.emv.GeneralEmvHandler
import com.payment.explorer.cardReader.model.APDU
import com.payment.explorer.cardReader.model.APDUSource
import com.payment.explorer.model.PaymentMethod
import com.payment.explorer.util.APDU_COMMAND_2PAY_SYS_DDF01
import com.payment.explorer.util.EMVUtils
import com.payment.explorer.util.TlvUtil

class EmvKernel(val context: Context, private val handler: EmvKernelCallback) : EmvKernelProvider {
    private lateinit var apduExchangeBridge: (cAPDU: APDU) -> APDU
    val core = KernelCore()
    private lateinit var emvHandler: BasicEmvHandler

    private fun initEmvHandler() {
        emvHandler = GeneralEmvHandler(this)
    }

    private fun setEmvHandler() {
        core.getICCTag("9F2A")?.let { kernelID ->
            emvHandler = when (kernelID.toInt()) {
                1 -> GeneralEmvHandler(this)
                else -> GeneralEmvHandler(this)
            }
        } ?: core.getICCTag("4F")?.let { aid ->
            emvHandler = when (EMVUtils.getPaymentMethodByAID(aid)) {
                PaymentMethod.MASTER -> GeneralEmvHandler(this)
                PaymentMethod.VISA -> GeneralEmvHandler(this)
                PaymentMethod.AMEX -> GeneralEmvHandler(this)
                PaymentMethod.JCB -> GeneralEmvHandler(this)
                PaymentMethod.DINERS, PaymentMethod.DISCOVER -> GeneralEmvHandler(this)
                PaymentMethod.UNIONPAY -> GeneralEmvHandler(this)
                else -> GeneralEmvHandler(this)
            }
        }
    }

    private fun onEmvProcessCompleted() {
        core.clearICCData()
        core.clearOdaData()
        core.clearTerminalData()
    }

    override fun loadTerminalData(terminalData: Map<String, String>) {
        core.saveTerminalData(terminalData)
    }

    override fun setApduExchangeBridge(bridge: (cAPDU: APDU) -> APDU) {
        apduExchangeBridge = bridge
    }

    override fun onTapEmvProcess() {
        handler.onEmvStarted()
        initEmvHandler()
        emvHandler.ppse()
        setEmvHandler()
        emvHandler.onTapProcess()
        handler.onTapEmvCompleted()
    }

    override fun postTapEmvProcess() {
        emvHandler.postTapProcess()
        handler.postTapEmvCompleted(core.cardData + core.terminalData)
        onEmvProcessCompleted()
    }

    override fun onError(exception: Exception) {
        core.clearICCData()
        core.clearOdaData()
        handler.onError(exception)
    }

    fun communicator(cmd: String): APDU {
        val cAPDU = APDU(cmd, APDUSource.TERMINAL)
        handler.onApduExchange(cAPDU)
        val rAPDU = apduExchangeBridge.invoke(cAPDU)
        handler.onApduExchange(rAPDU)
        return rAPDU
    }

    fun processTlv(tlv: String) {
        val decodedMap = TlvUtil.parseTLV(tlv)
        Log.d("processTlv", "tag data: $decodedMap")
        val tmp = decodedMap.mapValues { it.value.first() }
        Log.d("processTlv", "tags to be save: $tmp")
        core.saveICCData(tmp)
    }
}