package com.payment.explorer.ui.viewModel

import androidx.databinding.ObservableField
import com.payment.explorer.ui.base.BaseViewModel

class EmvKernelViewModel:BaseViewModel() {
    val promptMessage: ObservableField<String> = ObservableField()
}