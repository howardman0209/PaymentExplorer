package com.golden.template.ui.view.activity

import com.golden.template.R
import com.golden.template.databinding.ActivityTemplateBinding
import com.golden.template.ui.base.MVVMActivity
import com.golden.template.ui.viewModel.TemplateViewModel

class TemplateActivity: MVVMActivity<TemplateViewModel,ActivityTemplateBinding>() {


    override fun getViewModelInstance(): TemplateViewModel = TemplateViewModel()

    override fun setBindingData() {
        binding.viewModel = viewModel
        binding.view = this
    }

    override fun getLayoutResId(): Int = R.layout.activity_template
}