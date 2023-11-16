package com.payment.explorer.ui.view.activity

import android.content.Intent
import android.os.Bundle
import com.payment.explorer.R
import com.payment.explorer.databinding.ActivityTemplateBinding
import com.payment.explorer.ui.base.MVVMActivity
import com.payment.explorer.ui.viewModel.TemplateViewModel

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