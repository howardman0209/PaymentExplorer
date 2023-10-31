package com.hello.world.ui.view.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import com.hello.world.R
import com.hello.world.databinding.FragmentHomeBinding
import com.hello.world.ui.base.MVVMFragment
import com.hello.world.ui.viewModel.HomeViewModel
import com.hello.world.util.DebugPanelManager

class HomeFragment : MVVMFragment<HomeViewModel, FragmentHomeBinding>() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("HomeFragment", "onCreate")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        DebugPanelManager.log("HomeFragment - onViewCreated")
    }

    override fun getViewModelInstance(): HomeViewModel = HomeViewModel()

    override fun setBindingData() {
        binding.viewModel = viewModel
        binding.view = this
    }

    override fun getLayoutResId(): Int = R.layout.fragment_home

    override fun screenName(): String = "Home"
}