package com.payment.explorer.cardReader.android.nfc

import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.util.Log
import com.payment.explorer.cardReader.kernel.EmvKernelProvider
import com.payment.explorer.cardReader.model.APDU
import com.payment.explorer.cardReader.model.APDUSource
import com.payment.explorer.extension.sendAPDU

class EmvNfcCallback(private val emvKernel: EmvKernelProvider?) : NfcAdapter.ReaderCallback {
    override fun onTagDiscovered(p0: Tag?) {
        val isoDep = IsoDep.get(p0)

        try {
            isoDep?.connect()
            isoDep.timeout = 2000 // prevent slow card response
            Log.d("EmvNfcAdapterCallback", "isoDep: connected - $isoDep")
            emvKernel?.setApduExchangeBridge(
                bridge = { APDU(payload = isoDep.sendAPDU(it.payload), source = APDUSource.CARD) }
            )
            emvKernel?.onTapEmvProcess()
            isoDep?.close()
            Log.d("EmvNfcAdapterCallback", "IsoDep: closed")
        } catch (e: Exception) {
            Log.d("EmvNfcAdapterCallback", "Exception: $e")
            emvKernel?.onError(e)
        }
    }
}