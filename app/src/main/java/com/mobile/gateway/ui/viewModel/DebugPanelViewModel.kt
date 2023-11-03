package com.mobile.gateway.ui.viewModel

import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import com.mobile.gateway.ui.base.BaseViewModel

class DebugPanelViewModel : BaseViewModel() {
    val warpText = MutableLiveData(false)
    var isSearching = ObservableField(false)
    var searchStartIndex = 0
}