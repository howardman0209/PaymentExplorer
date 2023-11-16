package com.payment.explorer.ui.viewModel

import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import com.payment.explorer.ui.base.BaseViewModel

class DebugPanelViewModel : BaseViewModel() {
    val warpText = MutableLiveData(false)
    var isSearching = ObservableField(false)
    var searchStartIndex = 0
}