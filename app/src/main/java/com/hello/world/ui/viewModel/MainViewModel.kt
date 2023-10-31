package com.hello.world.ui.viewModel

import androidx.databinding.ObservableField
import com.hello.world.ui.base.BaseViewModel

class MainViewModel:BaseViewModel() {
    val title: ObservableField<String> = ObservableField()
}