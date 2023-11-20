package com.payment.explorer.ui.view.fragment

import com.payment.explorer.R
import com.payment.explorer.databinding.FragmentEmvKernelBinding
import com.payment.explorer.ui.base.MVVMFragment
import com.payment.explorer.ui.viewModel.EmvKernelViewModel

class EmvKernelFragment : MVVMFragment<EmvKernelViewModel, FragmentEmvKernelBinding>() {
    override fun getViewModelInstance(): EmvKernelViewModel = EmvKernelViewModel()

    override fun setBindingData() {
        binding.viewModel = viewModel
        binding.view = this
    }

    override fun getLayoutResId(): Int = R.layout.fragment_emv_kernel

    override fun screenName(): String = "EmvKernelFragment"
}