package com.mobile.gateway.ui.view.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.ViewModelProvider
import com.mobile.gateway.R
import com.mobile.gateway.databinding.ActivityMainBinding
import com.mobile.gateway.ui.base.MVVMActivity
import com.mobile.gateway.ui.view.fragment.DebugPanelFragment
import com.mobile.gateway.ui.view.fragment.HomeFragment
import com.mobile.gateway.ui.viewModel.MainViewModel
import com.mobile.gateway.util.DebugPanelManager

class MainActivity : MVVMActivity<MainViewModel, ActivityMainBinding>() {
    private var isDebug = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "onCreate")
        setSupportActionBar(binding.topAppBar)

        setUpDebugPanel()
        setUpMainContainer()

        binding.topAppBar.setNavigationOnClickListener {

        }

        DebugPanelManager.display.observe(this) {
            it.getContentIfNotHandled()?.let { visibility ->
                viewModel.debugPanelOn.set(visibility)
            }
        }

        DebugPanelManager.show(isDebug)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.tools, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.d("onOptionsItemSelected", "item: $item")
        when (item.itemId) {
            R.id.action_test -> {
//                val test = ""
//                LongLogUtil.debug("@@", "test: $test")
//                DebugPanelManager.log("test: $test")
            }

            R.id.tool_logcat -> {
//                isDebug = !isDebug
//                DebugPanelManager.show(isDebug)
                when (DebugPanelManager.debugPanelState.value) {
                    DebugPanelManager.DebugPanelState.EXPANDED -> DebugPanelManager.debugPanelState.postValue(DebugPanelManager.DebugPanelState.COLLAPSED)
                    DebugPanelManager.DebugPanelState.HALF_EXPANDED -> DebugPanelManager.debugPanelState.postValue(DebugPanelManager.DebugPanelState.EXPANDED)
                    else -> DebugPanelManager.debugPanelState.postValue(DebugPanelManager.DebugPanelState.HALF_EXPANDED)
                }
            }

            R.id.action_settings -> {
                startActivity(Intent(applicationContext, SettingActivity::class.java))
            }
        }
        return super.onOptionsItemSelected(item)
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

    override fun onBackPressed() {
        if (viewModel.debugPanelOn.get() != true) {
            yesNoDialog(this, "Leave this app?") {
                super.onBackPressed()
            }
        }
    }
}