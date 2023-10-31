package com.hello.world.ui.viewModel

import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import com.hello.world.ui.base.BaseViewModel

class DebugPanelViewModel : BaseViewModel() {
    val warpText = MutableLiveData(false)
    var isSearching = ObservableField(false)
    var searchStartIndex = 0
}