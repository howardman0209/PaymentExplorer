package com.payment.explorer.cardReader.kernel.emv

import android.util.Log
import com.payment.explorer.cardReader.kernel.EmvKernel
import com.payment.explorer.util.APDU_COMMAND_2PAY_SYS_DDF01
import com.payment.explorer.util.TlvUtil

abstract class BasicEmvHandler(private val kernel: EmvKernel) : EmvHandler {
    override fun ppse() {
        val apdu = kernel.communicator(APDU_COMMAND_2PAY_SYS_DDF01)
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
        finalTlv?.let {
            kernel.processTlv(it)
        } ?: throw Exception("NO_EMV_APP")
    }
}