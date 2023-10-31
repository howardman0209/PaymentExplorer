package com.hello.world.ui.view.activity

import android.content.Intent
import android.os.Bundle
import com.hello.world.R
import com.hello.world.databinding.ActivityTemplateBinding
import com.hello.world.ui.base.MVVMActivity
import com.hello.world.ui.viewModel.TemplateViewModel

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