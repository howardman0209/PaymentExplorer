package com.mobile.gateway.ui.view.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.mobile.gateway.MainApplication
import com.mobile.gateway.R
import com.mobile.gateway.databinding.ActivityMainBinding
import com.mobile.gateway.model.NavigationMenuData
import com.mobile.gateway.model.Tool
import com.mobile.gateway.model.getGroupList
import com.mobile.gateway.ui.base.MVVMActivity
import com.mobile.gateway.ui.view.fragment.DebugPanelFragment
import com.mobile.gateway.ui.view.fragment.HostFragment
import com.mobile.gateway.ui.view.viewAdapter.ExpandableMenuAdapter
import com.mobile.gateway.ui.viewModel.MainViewModel
import com.mobile.gateway.util.DebugPanelManager
import com.mobile.gateway.util.PreferencesUtil

class MainActivity : MVVMActivity<MainViewModel, ActivityMainBinding>() {
    private lateinit var menuAdapter: ExpandableMenuAdapter
    private var navigationMenuData: NavigationMenuData = MainApplication.getNavigationMenuData()
    private var isDebug = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "onCreate")
        setSupportActionBar(binding.topAppBar)
        setUpDebugPanel()

        binding.topAppBar.setNavigationOnClickListener {
            binding.drawerLayout.open()
        }

        menuAdapter = ExpandableMenuAdapter(
            this,
            navigationMenuData.getGroupList(),
            navigationMenuData.data
        )
        binding.expandableMenu.setAdapter(menuAdapter)

        binding.expandableMenu.setOnChildClickListener { expandableListView, view, groupPosition, childPosition, l ->
            val selectedGroup = navigationMenuData.getGroupList()[groupPosition]

            navigationMenuData.data[selectedGroup]?.get(childPosition)?.also { selectedTool ->
                Log.d("expandableMenu", "Selected group: $selectedGroup, selected tool: $selectedTool")
                setAppbarTitle(selectedTool)
                setUpMainContainer(selectedTool)
            }
            false
        }

        DebugPanelManager.display.observe(this) {
            it.getContentIfNotHandled()?.let { visibility ->
                viewModel.debugPanelOn.set(visibility)
            }
        }

        DebugPanelManager.show(isDebug)

        setAppbarTitle()
        setUpMainContainer()
    }

    private fun setAppbarTitle(tool: Tool = PreferencesUtil.getLastUsedTool(applicationContext)) {
        val title = getString(tool.resourceId)
        viewModel.title.set(title)
    }

    private fun setUpDebugPanel(target: Int = R.id.debugPanel) {
        pushFragment(DebugPanelFragment(), target, isAddToBackStack = false)
    }

    private fun setUpMainContainer(tool: Tool? = null) {
        if (tool == null) {
            // Auto expand tab for app launch
            navigationMenuData.also {
                val groupList = navigationMenuData.getGroupList()
                val lastUsedParentGroup = groupList.find { group ->
                    it.data[group]?.contains(PreferencesUtil.getLastUsedTool(applicationContext)) == true
                }
                binding.expandableMenu.post {
                    binding.expandableMenu.expandGroup(groupList.indexOf(lastUsedParentGroup))
                }
            }
        }

        val fragment: Fragment? = when (tool ?: PreferencesUtil.getLastUsedTool(applicationContext)) {
            Tool.HOST -> HostFragment()
            else -> null
        }
        fragment?.also {
            pushFragment(it, R.id.mainContainer, isAddToBackStack = false)
        }
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