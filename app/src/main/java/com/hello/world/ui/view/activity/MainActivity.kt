package com.hello.world.ui.view.activity

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.hello.world.R
import com.hello.world.databinding.ActivityMainBinding
import com.hello.world.ui.base.MVVMActivity
import com.hello.world.ui.view.fragment.DebugPanelFragment
import com.hello.world.ui.view.fragment.HomeFragment
import com.hello.world.ui.viewModel.MainViewModel
import com.hello.world.util.DebugPanelManager

class MainActivity : MVVMActivity<MainViewModel, ActivityMainBinding>() {
    private var isDebug = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "onCreate")
        DebugPanelManager.log("MainActivity - onCreate")
        setSupportActionBar(binding.topAppBar)

        setUpDebugPanel()
        setUpMainContainer()

        binding.topAppBar.setNavigationOnClickListener {
            isDebug = !isDebug
            DebugPanelManager.show(isDebug)
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