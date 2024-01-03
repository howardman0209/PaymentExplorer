package com.payment.explorer.ui.view.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.payment.explorer.MainApplication
import com.payment.explorer.R
import com.payment.explorer.databinding.ActivityMainBinding
import com.payment.explorer.model.NavigationMenuData
import com.payment.explorer.model.Tool
import com.payment.explorer.model.getGroupList
import com.payment.explorer.ui.base.MVVMActivity
import com.payment.explorer.ui.view.fragment.CardSimulatorFragment
import com.payment.explorer.ui.view.fragment.DebugPanelFragment
import com.payment.explorer.ui.view.fragment.EmvKernelFragment
import com.payment.explorer.ui.view.fragment.HostFragment
import com.payment.explorer.ui.view.viewAdapter.ExpandableMenuAdapter
import com.payment.explorer.ui.viewModel.MainViewModel
import com.payment.explorer.util.DebugPanelManager
import com.payment.explorer.util.PreferencesUtil

class MainActivity : MVVMActivity<MainViewModel, ActivityMainBinding>() {
    private lateinit var menuAdapter: ExpandableMenuAdapter
    private var navigationMenuData: NavigationMenuData = MainApplication.getNavigationMenuData()
    private var isDebug = true
    private var currentFragment: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "onCreate")
        setSupportActionBar(binding.topAppBar)
        setUpDebugPanel()

        binding.topAppBar.setNavigationOnClickListener {
            Log.d("MainActivity", "topAppBar On Click")
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

        binding.drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}

            override fun onDrawerOpened(drawerView: View) {}

            override fun onDrawerClosed(drawerView: View) {
                currentFragment?.let { pushFragment(it, R.id.mainContainer) }
            }

            override fun onDrawerStateChanged(newState: Int) {}
        })

        DebugPanelManager.display.observe(this) {
            it.getContentIfNotHandled()?.let { visibility ->
                viewModel.debugPanelOn.set(visibility)
            }
        }

        DebugPanelManager.show(isDebug)

        setAppbarTitle()
        setUpMainContainer()
        currentFragment?.let { pushFragment(it, R.id.mainContainer) }
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

        binding.drawerLayout.close()
        currentFragment = when (tool ?: PreferencesUtil.getLastUsedTool(applicationContext)) {
            Tool.HOST -> HostFragment()
            Tool.EMV_KERNEL -> EmvKernelFragment()
            Tool.CARD_SIMULATOR -> CardSimulatorFragment()
            else -> null
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