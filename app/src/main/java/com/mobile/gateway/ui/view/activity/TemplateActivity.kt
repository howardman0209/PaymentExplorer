package com.mobile.gateway.ui.view.activity

import android.content.Intent
import android.os.Bundle
import com.mobile.gateway.R
import com.mobile.gateway.databinding.ActivityTemplateBinding
import com.mobile.gateway.ui.base.MVVMActivity
import com.mobile.gateway.ui.viewModel.TemplateViewModel

class TemplateActivity : MVVMActivity<TemplateViewModel, ActivityTemplateBinding>() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = Intent(applicationContext, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun getViewModelInstance(): TemplateViewModel = TemplateViewModel()

    override fun setBindingData() {
        binding.viewModel = viewModel
        binding.view = this
    }

    override fun getLayoutResId(): Int = R.layout.activity_template
}