package com.mobile.gateway.ui.viewModel

import androidx.databinding.ObservableField
import com.mobile.gateway.ui.base.BaseViewModel

class MainViewModel : BaseViewModel() {
    var debugPanelOn = ObservableField(false)
    val title: ObservableField<String> = ObservableField()
}