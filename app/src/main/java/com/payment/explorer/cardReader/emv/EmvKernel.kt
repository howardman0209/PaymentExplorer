package com.payment.explorer.cardReader.emv

import android.util.Log
import com.payment.explorer.cardReader.model.APDU
import com.payment.explorer.cardReader.model.APDUSource
import com.payment.explorer.util.APDU_COMMAND_2PAY_SYS_DDF01
import com.payment.explorer.util.TlvUtil

class EmvKernel(private val handler: EmvKernelCallback) : EmvKernelProvider {
    private lateinit var apduExchangeBridge: (cAPDU: APDU) -> APDU
    override fun loadEmvConfig(emvConfig: HashMap<String, String>) {
        TODO("Not yet implemented")
    }

    override fun setApduExchangeBridge(bridge: (cAPDU: APDU) -> APDU) {
        apduExchangeBridge = bridge
    }

    fun communicator(apdu: APDU): APDU {
        handler.onApduExchange(apdu)
        val cAPDU = apduExchangeBridge.invoke(apdu)
        handler.onApduExchange(cAPDU)
        return cAPDU
    }

    override fun onTapEmvProcess() {
        handler.onEmvStarted()
        ppse()
        handler.onTapEmvCompleted()
    }

    override fun postTapEmvProcess() {

        handler.postTapEmvCompleted(emptyMap())
    }

    override fun onError(exception: Exception) {
        handler.onError(exception)
    }

    private fun ppse() {
        val apdu = communicator(APDU(APDU_COMMAND_2PAY_SYS_DDF01, APDUSource.TERMINAL))
        val appTemplates = TlvUtil.findByTag(apdu.payload, tag = "61")
        Log.d("ppse", "appTemplates: $appTemplates")
        val finalTlv = appTemplates?.let { appList ->
            // check if more than 1 aid return
            if (appList.size > 1) {
                Log.d("ppse", "multiple AID read from card")
                // CTL -> auto select app with higher Application Priority Indicator
                appList.minBy { TlvUtil.decodeTLV(it)["87"].toString().toInt(16) }
            } else {
                Log.d("ppse", "single AID read from card")
                appList.first()
            }
        }
        Log.d("ppse", "finalTlv: $finalTlv")
//        finalTlv?.let {
//            processTlv(it)
//        } ?: throw Exception("NO_EMV_APP")
    }
}