package com.payment.explorer.ui.viewModel

import androidx.databinding.ObservableField
import com.payment.explorer.ui.base.BaseViewModel

class EmvKernelViewModel:BaseViewModel() {
    val promptMessage: ObservableField<String> = ObservableField()
    val authAmount: ObservableField<String> = ObservableField()
    val cashbackAmount: ObservableField<String> = ObservableField()
}