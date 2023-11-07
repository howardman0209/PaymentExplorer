package com.mobile.gateway.ui.view.activity

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.mobile.gateway.R
import com.mobile.gateway.databinding.ActivityMainBinding
import com.mobile.gateway.ui.base.MVVMActivity
import com.mobile.gateway.ui.view.fragment.DebugPanelFragment
import com.mobile.gateway.ui.view.fragment.HomeFragment
import com.mobile.gateway.ui.viewModel.MainViewModel
import com.mobile.gateway.util.DebugPanelManager

class MainActivity : MVVMActivity<MainViewModel, ActivityMainBinding>() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "onCreate")
        DebugPanelManager.log("MainActivity - onCreate")
        setSupportActionBar(binding.topAppBar)

        setUpDebugPanel()
        setUpMainContainer()

        binding.topAppBar.setNavigationOnClickListener {

        }

        DebugPanelManager.display.observe(this) {
            it.getContentIfNotHandled()?.let { visibility ->
                binding.debugPanel.visibility = if (visibility) View.VISIBLE else View.GONE
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        return super.onCreateOptionsMenu(menu)
    }

    private fun setUpDebugPanel(target: Int = R.id.debugPanel) {
        pushFragment(DebugPanelFragment(), target, isAddToBackStack = false)
    }

    private fun setUpMainContainer(target: Int = R.id.mainContainer) {
        pushFragment(HomeFragment(), target, isAddToBackStack = false)
    }

    override fun getViewModelInstance() = ViewModelProvider(this)[MainViewModel::class.java]

    override fun setBindingData() {
        binding.viewModel = viewModel
        binding.view = this
    }

    override fun getLayoutResId(): Int = R.layout.activity_main
}