package com.payment.explorer.ui.viewModel

import androidx.databinding.ObservableField
import com.payment.explorer.ui.base.BaseViewModel

class MainViewModel : BaseViewModel() {
    var debugPanelOn = ObservableField(false)
    val title: ObservableField<String> = ObservableField()
}