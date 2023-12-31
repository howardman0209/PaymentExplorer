package com.mobile.gateway.ui.view.activity

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mobile.gateway.R
import com.mobile.gateway.databinding.ActivitySettingBinding
import com.mobile.gateway.databinding.DialogLogFontSettingBinding
import com.mobile.gateway.ui.base.MVVMActivity
import com.mobile.gateway.ui.viewModel.SettingViewModel
import com.mobile.gateway.util.PreferencesUtil

class SettingActivity : MVVMActivity<SettingViewModel, ActivitySettingBinding>() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("SettingActivity", "onCreate")

        binding.topAppBar.setNavigationOnClickListener {
            finish()
        }

        binding.settingFont.setOnClickListener {
            Log.d("SettingActivity", "settingFont")
            val dialogBinding: DialogLogFontSettingBinding = DataBindingUtil.inflate(LayoutInflater.from(this), R.layout.dialog_log_font_setting, null, false)
            dialogBinding.sliderFontSize.apply {
                //init view
                value = PreferencesUtil.getLogFontSize(applicationContext)
                setLabelFormatter { value ->
                    getString(R.string.var_font_size).format(value.toInt().toString())
                }
            }
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.label_setting_log_font_size)
                .setCancelable(false)
                .setView(dialogBinding.root)
                .setPositiveButton(R.string.button_confirm) { _, _ ->
                    Log.d("settingFont", "confirm")
                    Log.d("settingFont", "value: ${dialogBinding.sliderFontSize.value}")
                    PreferencesUtil.saveLogFontSize(applicationContext, dialogBinding.sliderFontSize.value)
                }
                .setNegativeButton(R.string.button_cancel) { _, _ ->
                    Log.d("settingFont", "cancel")
                }
                .show()
        }

        binding.settingDefaultPort.setOnClickListener {
            singleInputDialog(
                context = this,
                title = "Enter default port no.",
                fieldName = "port no.",
                fieldValue = PreferencesUtil.getDefaultPortNo(applicationContext)
            ) {
                PreferencesUtil.saveDefaultPortNo(applicationContext, it)
            }
        }
    }

    override fun getViewModelInstance(): SettingViewModel = ViewModelProvider(this)[SettingViewModel::class.java]
    override fun setBindingData() {
        binding.viewModel = viewModel
        binding.view = this
    }

    override fun getLayoutResId(): Int = R.layout.activity_setting
}