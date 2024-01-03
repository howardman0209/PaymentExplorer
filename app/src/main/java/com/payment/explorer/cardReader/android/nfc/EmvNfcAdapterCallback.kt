package com.payment.explorer.cardReader.android.nfc

import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.util.Log
import com.payment.explorer.cardReader.emv.EmvKernelProvider
import com.payment.explorer.extension.sendAPDU

class EmvNfcAdapterCallback(private val emvKernelProvider: EmvKernelProvider) : NfcAdapter.ReaderCallback {
    override fun onTagDiscovered(p0: Tag?) {
        val isoDep = IsoDep.get(p0)

        try {
            isoDep?.connect()
            Log.d("EmvNfcAdapterCallback", "isoDep: connected - $isoDep")
            emvKernelProvider.onTapEmvProcess { cAPDU ->
                isoDep.sendAPDU(cAPDU)
            }
            isoDep?.close()
            Log.d("EmvNfcAdapterCallback", "IsoDep: closed")
        } catch (e: Exception) {
            Log.d("EmvNfcAdapterCallback", "Exception: $e")
            emvKernelProvider.onError(e)
        }
    }
}