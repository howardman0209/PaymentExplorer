package com.payment.explorer.ui.view.fragment

import android.os.Bundle
import android.util.Log
import android.view.View
import com.payment.explorer.R
import com.payment.explorer.cardReader.CardReaderCallback
import com.payment.explorer.cardReader.android.AndroidCardReader
import com.payment.explorer.cardReader.model.APDU
import com.payment.explorer.cardReader.model.CardReaderStatus
import com.payment.explorer.databinding.FragmentEmvKernelBinding
import com.payment.explorer.ui.base.MVVMFragment
import com.payment.explorer.ui.viewModel.EmvKernelViewModel

class EmvKernelFragment : MVVMFragment<EmvKernelViewModel, FragmentEmvKernelBinding>() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val cardReader = AndroidCardReader(requireActivity(), object : CardReaderCallback {
            override fun updateReaderStatus(status: CardReaderStatus) {
                Log.d("updateReaderStatus", "CardReaderStatus: $status")
            }

            override fun onApduExchange(apdu: APDU) {
                Log.d("onApduExchange", "APDU: $apdu")
            }

            override fun onTransactionOnline(tlv: String) {
                Log.d("onTransactionOnline", "tlv: $tlv")
            }
        })

        cardReader.prepareEmvKernel()
        cardReader.prepareNfcDetector()
        cardReader.enableReader()
    }

    override fun getViewModelInstance(): EmvKernelViewModel = EmvKernelViewModel()

    override fun setBindingData() {
        binding.viewModel = viewModel
        binding.view = this
    }

    override fun getLayoutResId(): Int = R.layout.fragment_emv_kernel

    override fun screenName(): String = "EmvKernelFragment"
}