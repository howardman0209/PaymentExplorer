package com.payment.explorer.ui.view.fragment

import com.payment.explorer.R
import com.payment.explorer.databinding.FragmentCardSimulatorBinding
import com.payment.explorer.ui.base.MVVMFragment
import com.payment.explorer.ui.viewModel.CardSimulatorViewModel

class CardSimulatorFragment : MVVMFragment<CardSimulatorViewModel, FragmentCardSimulatorBinding>() {
    override fun getViewModelInstance(): CardSimulatorViewModel = CardSimulatorViewModel()

    override fun setBindingData() {
        binding.viewModel = viewModel
        binding.view = this
    }

    override fun getLayoutResId(): Int = R.layout.fragment_card_simulator

    override fun screenName(): String = "CardSimulatorFragment"
}